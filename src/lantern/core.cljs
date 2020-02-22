(ns lantern.core
    (:require
     [reagent.core :as r]
     [cljs.reader :refer [read-string]]))

(defn add-class [id class]
  (.add (.-classList (.getElementById js/document id))
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
           (when (= (.-animationName style) "")
             (set! (.-animationName style) (str "spinner-" book)))
           (.remove (.-classList node) "normal"))
         1000)
        (js/setTimeout #(.remove (.-classList node) "closing") 5000)))))

(defn make-book [[book spine-width pages]]
  (let [images (atom {})
        shrink 4
        height (/ 2256 shrink)
        width (/ 1392 shrink)
        spine-width (/ spine-width shrink)
        state (atom :spinning)
        ears (js->clj js/earses)
        ear (nth ears (rand (count ears)))
        id (str "book" book)]
    (list
     images
     (str id "cont")
     [:div.book-container {:id (str id "cont")}
      [:div.book
       {:on-click #(read-book book id state)
        :id id
        :style {:animation-duration (str (+ (rand 10) 5) "s")
                :animation-name (str "spinner-" book)
                :width (px width)
                :height (px height)}}
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
                     {:id (str "page" page)
                      :style
                      {:width (px width)
                       :height (px height)
                       :background-image (img images
                                              (str book "/p0" pic ".jpg"))
                       :background-size (str (px (* width 2)) " " (px height))
                       :transform-origin "left top"
                       :background-position (str (px (- width)) " "
                                                 (px height))}}]]
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
                     {:id (str "page" page)
                      :style
                      {:width (px width)
                       :height (px height)
                       :background-image (img images
                                              (str book "/p0" pic ".jpg"))
                       :background-size (str (px (* width 2)) " " (px height))
                       :transform-origin "right bottom"}}]])))
             (reverse (range 0 7))))
       [:div.face
        {:style {:width (px width)
                 :height (px height)
                 :transform (trans (y 0) (tz (/ spine-width 2)))}}
        [:div.face.front
         {:style {:background-image (img images (str book "/01.jpg"))
                  :width (px width)
                  :height (px height)
                  :background-size (str (px width) " " (px height))
                  :transform-origin "left top"}}]]
       [:div.face.back
        {:style {:background-image (img images (str book "/02.jpg"))
                 :width (px width)
                 :height (px height)
                 :background-size (str (px width) " " (px height))
                 :transform (trans (y 180) (tz (/ spine-width 2)))}}]
       [:div.face.left
        {:style {:background-image (img images (str book "/03.jpg"))
                 :width (px spine-width)
                 :height (px height)
                 :background-size (str (px spine-width) " " (px height))
                 :left (px (- (/ width 2) (/ spine-width 2)))
                 :transform (trans (y -90) (tz (/ width 2)))}}]
       [:div.face.right
        {:style {:background-image (img images (str "pages/" ear "/01.jpg"))
                 :width (px spine-width)
                 :height (px height)
                 :background-size (str (px spine-width) " " (px height))
                 :left (px (- (/ width 2) (/ spine-width 2)))
                 :transform (trans (y 90) (tz (/ width 2)))}}]
       [:div.face.top
        {:style {:background-image (img images (str "pages/" ear "/02.jpg"))
                 :width (px width)
                 :height (px spine-width)
                 :background-size (str (px width) " " (px spine-width))
                 :top (px (- (/ height 2) (/ spine-width 2)))
                 :transform (trans (x 90) (tz (/ height 2)))}}]
       [:div.face.bottom
        {:style {:background-image (img images (str "pages/" ear "/03.jpg"))
                 :width (px width)
                 :height (px spine-width)
                 :background-size (str (px width) " " (px spine-width))
                 :top (px (- (/ height 2) (/ spine-width 2)))
                 :transform (trans (x -90) (tz (/ height 2)))}}]]])))

(defn wait-for-images [[images id html]]
  (let [loaded (fn [url]
                 (swap! images conj {url :loaded})
                 (when (every? (fn [[_ status]]
                                 (= status :loaded))
                               @images)
                   (let [node (.getElementById js/document id)]
                     (.add (.-classList node) "fade-in"))))]
    [:div
     html
     [:div {:style {:display "none"}}
      (map (fn [[url state]]
             [:img {:key url
                    :on-load #(loaded url)
                    :src url}])
           @images)]]))

(defn spinning []
  (let [bs (js->clj js/books)]
    [:div
     [:h2 "Lantern"]
     (wait-for-images (make-book (nth bs 43)))]))

(defn take-out-library-book [id book width]
  (prn id)
  (r/render (wait-for-images (make-book [book width 8]))
            (.getElementById js/document (str "take-out-" book))))

(defn make-library [books]
  (let [shrink 8]
    [:div.library
     (map (fn [[book width _]]
            (let [id (str "library-book-" book)]
              [:div.library-book {:key book
                                  :on-click #(take-out-library-book
                                              id book width)
                                  :id id}
               [:img {:on-load (fn []
                                 (add-class id "fade-in"))
                      :src (image-url (str book "/03.jpg"))
                      :width (/ width shrink)
                      :height (/ 2256 shrink)}]]))
          (sort #(compare (read-string (first %1))
                          (read-string (first %2)))
                books))]))

(defn library []
  (let [bs (js->clj js/books)]
    [:div
     [:h2 "Library"]
     (make-library bs)
     [:div.take-outs
      (map (fn [[book _ _]]
             [:div.take-out {:id (str "take-out-" book)
                             :key (str "take-out-" book)}])
           bs)]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [library] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
