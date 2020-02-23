(ns lantern.core
    (:require
     [reagent.core :as r]
     [cljs.reader :refer [read-string]]))

(defn add-class [id class]
  (.add (.-classList (.getElementById js/document id))
        class))

(defn remove-class [id class]
  (.remove (.-classList (.getElementById js/document id))
           class))

(defn px [number]
  (str number "px"))

(defn image-url [string]
  (str "https://quimby.gnus.org/circus/lanterne/" string))

(defn img [images image]
  (let [url (image-url image)]
    ;; Save images so that we can later check that they loaded.
    (when images
      (swap! images conj {url :new}))
    (str "url('" url "')")))

(defn tz [z]
  (str "translateZ(" z "px)"))

(defn x [x]
  (str "rotateX(" x "deg)"))

(defn y [y]
  (str "rotateY(" y "deg)"))

(defn z [z]
  (str "rotateZ(" z "deg)"))

(defn trans [& elems]
  (reduce str (interpose " " elems)))

(defn randoms []
  (sort (map (fn [_]
               (Math/floor (rand 360)))
             (range 0 5))))

(defn make-keyframes [name]
  (reduce #'str
          (conj
           (concat
            (map (fn [i xv yv zv]
                   (str (* i 20) "% { transform: "
                        (trans (x xv) (y yv) (z zv))
                        "; } "))
                 (range 1 5)
                 (randoms)
                 (randoms)
                 (randoms))
            (list "100% { transform: translateZ(-50px) rotateY(360deg) rotateX(360deg) rotateZ(360deg); }}"))
           (str "@keyframes spinner-" name " { 0% { transform: translateZ(-50px) rotateY(0deg) rotateX(0deg) rotateZ(0deg); } "))))                     

(defn read-book [book id state]
  (let [node (.getElementById js/document id)
        style (.-style node)]
    (prn state)
    (cond
      (= @state :spinning)
      (do
        ;; Set the current animation 3d transform so we have something to
        ;; transition from.
        (set! (.-transform style) (js/window.getRotation node))
        (set! (.-animationName style) "")
        (reset! state :front)
        ;; Chrome needs to do a reflow before adding the transition class.
        (js/setTimeout #(.add (.-classList node) "see-front") 10))
      (= @state :front)
      (do
        (reset! state :back)
        (.remove (.-classList node) "see-front")
        (.add (.-classList node) "see-back"))
      (= @state :back)
      (do
        (reset! state :open-1)
        (.remove (.-classList node) "see-back")
        (.add (.-classList node) "open-1"))
      (= @state :open-1)
      (do
        (reset! state :open-2)
        (.remove (.-classList node) "open-1")
        (.add (.-classList node) "open-2"))
      (= @state :open-2)
      (do
        (reset! state :open-3)
        (.remove (.-classList node) "open-2")
        (.add (.-classList node) "open-3"))
      (= @state :open-3)
      (do
        (reset! state :spinning)
        (.remove (.-classList node) "open-3")
        (.add (.-classList node) "normal")
        (.add (.-classList node) "closing")
        (js/setTimeout
         (fn []
           (prn "Starting to spin")
           (when (= (.-animationName style) "")
             (set! (.-animationName style) (str "spinner-" book)))
           (.remove (.-classList node) "normal"))
         1000)
        (js/setTimeout #(.remove (.-classList node) "closing") 5000)))))

(defn make-book [[book spine-width pages spines-only on-click]]
  (let [images (atom {})
        shrink 4
        height (/ 2256 shrink)
        width (/ 1392 shrink)
        spine-width (/ spine-width shrink)
        state (atom (if spines-only :spine :spinning))
        ears (js->clj js/earses)
        ear (nth ears (rand (count ears)))
        image-property (if spines-only
                         :background-url
                         :background-image)
        simg (fn [images file]
               (img (if spines-only nil images) file))
        id (str "book" book)]
    (list
     images
     (str id "cont")
     [:div.book-container {:id (str id "cont")}
      [:div.book
       {:on-click (if on-click
                    (on-click state)
                    #(read-book book id state))
        :id id
        :style {:animation-duration (str (+ (rand 10) 5) "s")
                :transform (trans (y 90)
                                  (x 5)
                                  (str "translateZ("
                                       (/ spine-width shrink) "px)")
                                  (str " scale(0.5)")
                                  (str " scaleZ(0.5)"))
                :animation-name (if spines-only
                                  ""
                                  (str "spinner-" book))}}
       ;; The spinner animation keyframes.
       [:style (make-keyframes book)]
       ;; The interior pages.
       (doall
        (map (fn [page]
               (let [pic (Math.floor (/ page 2))]
                 (if (odd? page)
                   [:div.face
                    {:key (str "page" page)
                     :style
                     {:width (px width)
                      :height (px height)
                      :transform (trans (tz (- (/ spine-width 2)
                                               (+ (/ page 10) 0.1)))
                                        (x 0))}}
                    [:div.face.page
                     {:style
                      {:width (px width)
                       :height (px height)
                       image-property (simg images (str book "/p0" pic ".jpg"))
                       :background-size (str (px (* width 2)) " " (px height))
                       :transform-origin "left top"
                       :background-position (str (px (- width)) " "
                                                 (px height))}
                      :id (str "book" book (str "page" page))}]]
                   ;; Even pages.
                   [:div.face
                    {:key (str "page" page)
                     :style {:width (px width)
                             :height (px height)
                             :transform (trans (z 180)
                                               (tz (- (/ spine-width 2)
                                                      (+ (/ page 10) 0.1)))
                                               (x 180))}}
                    [:div.face.page
                     {:style
                      {:width (px width)
                       :height (px height)
                       image-property (simg images (str book "/p0" pic ".jpg"))
                       :background-size (str (px (* width 2)) " " (px height))
                       :transform-origin "right bottom"}
                      :id (str "book" book (str "page" page))}]])))
             (reverse (range 0 7))))
       [:div.face
        {:style {:width (px width)
                 :height (px height)
                 :transform (trans (y 0) (tz (/ spine-width 2)))}}
        [:div.face.front
         {:style {image-property (simg images (str book "/01.jpg"))
                  :width (px width)
                  :height (px height)
                  :background-size (str (px width) " " (px height))
                  :transform-origin "left top"}
          :id (str "book" book "front")}]]
       [:div.face.back
        {:style {image-property (simg images (str book "/02.jpg"))
                 :width (px width)
                 :height (px height)
                 :background-size (str (px width) " " (px height))
                 :transform (trans (y 180) (tz (/ spine-width 2)))}
         :id (str "book" book "back")}]
       [:div.face.left
        {:style {:background-image (img images (str book "/03.jpg"))
                 :width (px spine-width)
                 :height (px height)
                 :background-size (str (px spine-width) " " (px height))
                 :left (px (- (/ width 2) (/ spine-width 2)))
                 :transform (trans (y -90) (tz (/ width 2)))}
         :id (str "book" book "left")}]
       [:div.face.right
        {:style {image-property (simg images (str "pages/" ear "/01.jpg"))
                 :width (px spine-width)
                 :height (px height)
                 :background-size (str (px spine-width) " " (px height))
                 :left (px (- (/ width 2) (/ spine-width 2)))
                 :transform (trans (y 90) (tz (/ width 2)))}
         :id (str "book" book "right")}]
       [:div.face.top
        {:style {image-property (simg images (str "pages/" ear "/02.jpg"))
                 :width (px width)
                 :height (px spine-width)
                 :background-size (str (px width) " " (px spine-width))
                 :top (px (- (/ height 2) (/ spine-width 2)))
                 :transform (trans (x 90) (tz (/ height 2)))}
         :id (str "book" book "top")}]
       [:div.face.bottom
        {:style {image-property (img images (str "pages/" ear "/03.jpg"))
                 :width (px width)
                 :height (px spine-width)
                 :background-size (str (px width) " " (px spine-width))
                 :top (px (- (/ height 2) (/ spine-width 2)))
                 :transform (trans (x -90) (tz (/ height 2)))}
         :id (str "book" book "bottom")}]]])))

(defn wait-for-images [[images id html] & callback]
  (let [loaded (fn [node url]
                 (swap! images conj {url :loaded})
                 (when (every? (fn [[_ status]]
                                 (= status :loaded))
                               @images)
                   (if callback
                     ;; Call the provided callback.
                     ((first callback))
                     ;; The default callback.
                     (let [node (.getElementById js/document id)]
                       (.add (.-classList node) "fade-in")))))]
    [:div {:key (first (first @images))}
     html
     [:div {:style {:display "none"}}
      (map (fn [[url state]]
              [:img {:key url
                     :on-load #(loaded %1 url)
                     :src url}])
           @images)]]))

(defn spinning []
  (let [bs (js->clj js/books)]
    [:div
     [:h2 "Lantern"]
     (wait-for-images (make-book (nth bs 43)))]))

(defn set-background-image [images class book]
  (let [style (.-style (.getElementById js/document
                                        (str "book" book class)))
        spec (.-backgroundUrl style)
        [_ url] (re-find #"'(.*)'" spec)]
    (swap! images conj {url :new})
    ;; Copy over the URLs we computed to the real slots so that the
    ;; browser will load them.
    (set! (.-backgroundImage style) spec)))

(defonce book-z-index (atom 1))

(defn take-out-library-book [id book width state]
  (prn id state)
  (cond
    (= @state :spine)
    (let [node (.getElementById js/document id)
          done (atom false)
          style (.-style node)]
      (reset! state :spinning)
      (add-class (str "book" book) "take-out-slide")
      (set! (.-zIndex (.-style (.getElementById js/document
                                                (str "library-book-" book))))
            @book-z-index)
      (swap! book-z-index inc)
      (let [images (atom {})]
        (doseq [class '("front" "back" "right" "top" "bottom")]
          (set-background-image images class book))
        (r/render (wait-for-images
                   [images nil [:div]]
                   (fn []
                     (when (not @done)
                       (reset! done true)
                       (prn (str "loaded" book))
                       (add-class (str "book" book) "take-out-loaded"))))
                  (.getElementById js/document (str "preload-" book)))))
    true (read-book book id state)))

(defn make-library [books]
  (let [shrink 8]
    [:div.library
     (map (fn [[book width pages]]
            (let [id (str "library-book-" book)
                  [images book-id html]
                  (make-book
                   [book width pages true
                    (fn [state]
                      (fn []
                        (take-out-library-book id book width state)))])]
              [:div.library-book {:key book
                                  :id id
                                  :style {:width (px (+ (/ width shrink) 1))
                                          :height (px (/ 2256 shrink))}}
               html
               [:div {:style {:display "none"}}
                (map (fn [[url state]]
                       [:img {:key url
                              :on-load (fn []
                                         (add-class id "fade-in"))
                              :src url}])
                     @images)]]))
          (sort #(compare (read-string (first %1))
                          (read-string (first %2)))
                books))]))

(defn library []
  (let [bs (js->clj js/books)]
    [:div
     [:h2 "Library"]
     (make-library bs)
     [:div#load-images (map (fn [[book _ _]]
                              [:div {:id (str "preload-" book)
                                     :key (str "preload-" book)}])
                            bs)]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [library] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
