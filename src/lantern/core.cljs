(ns lantern.core
    (:require
      [reagent.core :as r]))

;; -------------------------
;; Views


(defn px [number]
  (str number "px"))

(defn img [image & offset]
  (str "url('https://quimby.gnus.org/circus/lanterne/" image "')"
       (if offset
         " -200px -200px"
         "")))

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
  (let [shrink 4
        height (/ 2256 shrink)
        width (/ 1392 shrink)
        spine-width (/ spine-width shrink)
        state (r/atom :spinning)
        ears (js->clj js/earses)
        ear (nth ears (rand (count ears)))
        id (str "book" book)]
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
     (map (fn [page]
            (let [pic (+ (Math.floor (/ page 2)) 1)]
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
                    :background-image (img (str book "/p0" pic ".jpg"))
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
                    :background-image (img (str book "/p0" pic ".jpg"))
                    :background-size (str (px (* width 2)) " " (px height))
                    :transform-origin "right bottom"}}]])))
          (reverse (range 0 7)))
     [:div.face
      {:style {:width (px width)
               :height (px height)
               :transform (trans (y 0) (tz (/ spine-width 2)))}}
      [:div.face.front
       {:style {:background-image (img (str book "/01.jpg"))
                :width (px width)
                :height (px height)
                :background-size (str (px width) " " (px height))
                :transform-origin "left top"}}]]
     [:div.face.back
      {:style {:background-image (img (str book "/02.jpg"))
               :width (px width)
               :height (px height)
               :background-size (str (px width) " " (px height))
               :transform (trans (y 180) (tz (/ spine-width 2)))}}]
     [:div.face.left
      {:style {:background-image (img (str book "/03.jpg"))
               :width (px spine-width)
               :height (px height)
               :background-size (str (px spine-width) " " (px height))
               :left (px (- (/ width 2) (/ spine-width 2)))
               :transform (trans (y -90) (tz (/ width 2)))}}]
     [:div.face.right
      {:style {:background-image (img (str "pages/" ear "/01.jpg"))
               :width (px spine-width)
               :height (px height)
               :background-size (str (px spine-width) " " (px height))
               :left (px (- (/ width 2) (/ spine-width 2)))
               :transform (trans (y 90) (tz (/ width 2)))}}]
     [:div.face.top
      {:style {:background-image (img (str "pages/" ear "/02.jpg"))
               :width (px width)
               :height (px spine-width)
               :background-size (str (px width) " " (px spine-width))
               :top (px (- (/ height 2) (/ spine-width 2)))
               :transform (trans (x 90) (tz (/ height 2)))}}]
     [:div.face.bottom
      {:style {:background-image (img (str "pages/" ear "/03.jpg"))
               :width (px width)
               :height (px spine-width)
               :background-size (str (px width) " " (px spine-width))
               :top (px (- (/ height 2) (/ spine-width 2)))
               :transform (trans (x -90) (tz (/ height 2)))}}]]))

(defn home-page []
  (let [bs (js->clj js/books)]
    [:div
     [:h2 "Lantern"]
     (make-book (nth bs 10))]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
