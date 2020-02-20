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

(defn make-book [[book spine-width pages]]
  (let [height 564
        width 348
        spine-width (/ spine-width 4)]
    (prn [book spine-width pages])
    [:div.book
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
      {:style {:background-image (img "pages/01.jpg")
               :width (px spine-width)
               :height (px height)
               :background-size (str (px spine-width) " " (px height))
               :left (px (- (/ width 2) (/ spine-width 2)))
               :transform (trans (y 90) (tz (/ width 2)))}}]
     [:div.face.top
      {:style {:background-image (img "pages/02-rot.jpg")
               :width (px width)
               :height (px spine-width)
               :background-size (str (px width) " " (px spine-width))
               :top (px (- (/ height 2) (/ spine-width 2)))
               :transform (trans (x 90) (tz (/ height 2)))}}]
     [:div.face.bottom
      {:style {:background-image (img "pages/03-rot.jpg")
               :width (px width)
               :height (px spine-width)
               :background-size (str (px width) " " (px spine-width))
               :top (px (- (/ height 2) (/ spine-width 2)))
               :transform (trans (x -90) (tz (/ height 2)))}}]
     (map (fn [page]
            (if (odd? page)
              [:div.face
               {:key (str "page" page 1)
                :style {:width (px width)
                        :height (px height)
                        :transform (trans (tz (- (/ spine-width 2) 0.2))
                                          (x 0))}}
               [:div.face.page
                {:id (str "page" page 1)
                 :style
                 {:width (px width)
                  :height (px height)
                  :background-image (img (str book "/p01.jpg"))
                  :background-size (str (px (* width 2)) " " (px height))
                  :background-position (str (px (- width)) " " (px height))}}]]
              ;; Odd pages.
              [:div.face
               {:key (str "page" page)
                :style {:width (px width)
                        :height (px height)
                        :transform (trans (z 180)
                                          (tz (- (/ spine-width 2) 0.1))
                                          (x 180))}}
               [:div.face.page
                {:id (str "page" page)
                 :style
                 {:width (px width)
                  :height (px height)
                  :background-image (img (str book "/p01.jpg"))
                  :background-size (str (px (* width 2)) " " (px height))
                  :transform-origin "right bottom"}}]]))
          (range 0 2))]))

(defn home-page []
  (let [bs (js->clj js/books)]
    [:div
     [:h2 "Lantern"]
     (make-book (nth bs 1))]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
