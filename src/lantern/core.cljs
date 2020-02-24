(ns lantern.core
    (:require
     [reagent.core :as r]
     [cljs.reader :refer [read-string]]))

(defn book-id [book]
  (str "book" book))

(defn cont-id [book]
  (str "book" book "cont"))

(defn page-id [book page]
  (str "book" book page))

(defn find-node [id]
  (.getElementById js/document id))

(defn find-style [id]
  (.-style (.getElementById js/document id)))

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

(defn make-spinner [name]
  (reduce #'str
          (conj
           (concat
            (map (fn [i xv yv zv]
                   (str (* i 20) "% { transform: "
                        (trans (tz 400) (x xv) (y yv) (z zv))
                        "; } "))
                 (range 1 5)
                 (randoms)
                 (randoms)
                 (randoms))
            (list "100% { transform: translateZ(200px) rotateY(360deg) rotateX(360deg) rotateZ(360deg); }}"))
           (str "@keyframes spinner-" name " { 0% { transform: translateZ(200px) rotateY(0deg) rotateX(0deg) rotateZ(0deg); } "))))                     

(defn read-book [book id state]
  (let [node (.getElementById js/document id)
        style (.-style node)]
    (prn state id)
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
        id (book-id book)
        oc nil]
    (list
     images
     (cont-id book)
     [:div.book-container {:id (cont-id book)}
      [:div.book
       {:id id
        :on-click (if on-click
                    (on-click state)
                    #(read-book book id state))
        :style (if spines-only
                 {:animation-duration (str (+ (rand 10) 5) "s")
                  :transform (trans (tz 0)
                                    (str "translateX("
                                         (/ spine-width shrink) "px)")
                                    (y 90)
                                    (x 5)
                                    (str " scale(0.5)")
                                    (str " scaleZ(0.5)"))}
                 {:animation-duration (str (+ (rand 10) 5) "s")
                  :width (px width)
                  :height (px height)
                  :animation-name (str "spinner-" book)})}
       ;; The spinner animation keyframes.
       [:style (make-spinner book)]
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
                    [:div
                     {:style
                      {:width (px width)
                       :height (px height)
                       image-property (simg images (str book "/p0" pic ".jpg"))
                       :background-size (str (px (* width 2)) " " (px height))
                       :transform-origin "left top"
                       :background-position (str (px (- width)) " "
                                                 (px height))}
                      :class (str "face page page" page)
                      ;; The click here doesn't work?
                      :on-click oc
                      :id (page-id book (str "page" page))}]]
                   ;; Even pages.
                   [:div.face
                    {:key (str "page" page)
                     :style {:width (px width)
                             :height (px height)
                             :transform (trans (z 180)
                                               (tz (- (/ spine-width 2)
                                                      (+ (/ page 10) 0.1)))
                                               (x 180))}}
                    [:div
                     {:style
                      {:width (px width)
                       :height (px height)
                       image-property (simg images (str book "/p0" pic ".jpg"))
                       :background-size (str (px (* width 2)) " " (px height))
                       :transform-origin "right bottom"}
                      :class (str "face page page" page)
                      :on-click oc
                      :id (page-id book (str "page" page))}]])))
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
          :id (page-id book "front")
          :on-click oc}]]
       [:div.face.back
        {:style {image-property (simg images (str book "/02.jpg"))
                 :width (px width)
                 :height (px height)
                 :background-size (str (px width) " " (px height))
                 :transform (trans (y 180) (tz (/ spine-width 2)))}
         :id (page-id book "back")
         :on-click oc}]
       [:div.face.left
        {:style {:background-image (img images (str book "/03.jpg"))
                 :width (px spine-width)
                 :height (px height)
                 :background-size (str (px spine-width) " " (px height))
                 :left (px (- (/ width 2) (/ spine-width 2)))
                 :transform (trans (y -90) (tz (/ width 2)))}
         :id (page-id book "left")
         :on-click oc}]
       [:div.face.right
        {:style {image-property (simg images (str "pages/" ear "/01.jpg"))
                 :width (px spine-width)
                 :height (px height)
                 :background-size (str (px spine-width) " " (px height))
                 :left (px (- (/ width 2) (/ spine-width 2)))
                 :transform (trans (y 90) (tz (/ width 2)))}
         :id (page-id book "right")
         :on-click oc}]
       [:div.face.top
        {:style {image-property (simg images (str "pages/" ear "/02.jpg"))
                 :width (px width)
                 :height (px spine-width)
                 :background-size (str (px width) " " (px spine-width))
                 :top (px (- (/ height 2) (/ spine-width 2)))
                 :transform (trans (x 90) (tz (/ height 2)))}
         :id (page-id book "top")
         :on-click oc}]
       [:div.face.bottom
        {:style {image-property (img images (str "pages/" ear "/03.jpg"))
                 :width (px width)
                 :height (px spine-width)
                 :background-size (str (px width) " " (px spine-width))
                 :top (px (- (/ height 2) (/ spine-width 2)))
                 :transform (trans (x -90) (tz (/ height 2)))}
         :id (page-id book "bottom")
         :on-click oc}]]])))

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
     (wait-for-images (make-book (nth bs 40)))]))

(defn set-background-image [images class book]
  (let [style (find-style (page-id book class))
        spec (.-backgroundUrl style)
        [_ url] (re-find #"'(.*)'" spec)]
    (swap! images conj {url :new})
    ;; Copy over the URLs we computed to the real slots so that the
    ;; browser will load them.
    (set! (.-backgroundImage style) spec)))

(defn make-center [book spine-width]
  (let [pos (js->clj (js/getPosition (find-node (str "library-book-" book))))]
    (set! (.-innerHTML (find-node (str (str "library-book-style-" book))))
          (str ".center-" book " { transition: all 2s; transition-timing-function: linear; margin-left: "
               (- (/ (.-innerWidth js/window) 2) (first pos))
               "px; margin-top: "
               (+ (.-scrollTop (find-node "html"))
                  (- (/ (.-innerHeight js/window) 2)
                     (nth pos 1)
                     (/ 2256 8)))
               "px; }"
               ".put-back-book-" book " { transition: all 1s; transform: translateZ(0px) translateX("
               (/ spine-width 16)
               "px) rotateY(90deg) rotateX(5deg) scale(0.5) scaleZ(0.5) !important; width: 0px !important; height: 0px !important; }"))))

(defonce book-z-index (atom 1))

(defn take-out-library-book [id book width state]
  (prn "taking out" id state)
  (set! (.-zIndex (find-style id)) (swap! book-z-index inc))
  (cond
    (= @state :put-back)
    (do
      (reset! state :front)
      (remove-class (book-id book) (str "put-back-book-" book))
      (remove-class (cont-id book) "put-back-cont")
      (add-class (book-id book) "see-front"))
    (= @state :spine)
    (let [done (atom false)
          start (.getTime (js/Date.))
          style (find-style id)]
      (add-class (book-id book) "take-out-slide")
      ;;(set! (.-zIndex style) (swap! book-z-index inc))
      (let [images (atom {})]
        (doseq [class '("front" "back" "right" "top" "bottom"
                        "page0" "page1" "page2" "page3" "page4" "page5"
                        "page6")]
          (set-background-image images class book))
        (r/render
         (wait-for-images
          [images nil [:div]]
          (fn []
            (when (not @done)
              (reset! done true)
              (prn (str "loaded" book))
              (let [loaded
                    (fn []
                      (let [style (find-style (book-id book))
                            cont-style (find-style (cont-id book))]
                        (remove-class (book-id book) "take-out-slide")
                        (reset! state :front)
                        (set! (.-width style) (px (/ 1392 4)))
                        (set! (.-left cont-style) (px (- (/ 1392 8))))
                        (add-class (book-id book) "see-front")
                        (make-center book width)
                        (add-class (cont-id book) (str "center-" book))
                        (js/setTimeout
                         (fn []
                           (set! (.-height style) (px (/ 2256 4)))
                           ;;(set! (.-top cont-style) (px (/ 2256 4)))
                           )
                         2000)))
                    lapsed (- (.getTime (js/Date.)) start)]
                ;; Always give the first transition (pulling
                ;; the book out) at least one second before
                ;; turning.
                (if (< lapsed 1000)
                  (js/setTimeout loaded (- 1000 lapsed))
                  (loaded))))))
         (.getElementById js/document (str "preload-" book)))))
    (= @state :spinning)
    (do
      ;; Set the current animation 3d transform so we have something to
      ;; transition from.
      (let [node (find-node (book-id book))
            style (.-style node)]
        (set! (.-transform style) (js/window.getRotation node))
        (set! (.-animationName style) "")
        (reset! state :put-back)
        (prn "Putting back in")
        ;; Chrome needs to do a reflow before adding the transition class.
        (js/setTimeout (fn []
                         (add-class (book-id book) "put-back-book")
                         (add-class (cont-id book) "put-back-cont")
                         (js/setTimeout
                          (fn []
                            (remove-class (book-id book) "put-back-book")
                            (add-class (book-id book) (str "put-back-book-" book)))
                          3000))
                       10)))
    true (read-book book (book-id book) state)))

(defn make-library [books]
  (let [shrink 8]
    [:div.library
     (map (fn [[book width pages]]
            (let [id (str "library-book-" book)
                  [images book-id html]
                  (make-book
                   [book width pages true
                    (fn [state]
                      (fn [e]
                        (take-out-library-book id book width state)))])]
              [:div.library-book {:key book
                                  :id id
                                  :style {:width (px (+ (/ width shrink) 1))
                                          :height (px (/ 2256 shrink))}}
               html
               [:style {:id (str "library-book-style-" book)}]
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
