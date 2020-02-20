(ns lantern.core
    (:require
      [reagent.core :as r]))

;; -------------------------
;; Views

(defn px [number]
  (str number "px"))

(defn img [image]
  (str "url(https://quimby.gnus.org/circus/lanterne/" image ")"))

(defn make-book [[book spine-width pages]]
  (let [height 564
        width 348
        spine-width (/ spine-width 4)]
    (prn [book spine-width pages])
    [:div.book
     [:div.face.front
      {:style {:background-image (img (str book "/01.jpg"))
               :width (px width)
               :height (px height)
               :background-size (str (px width) " " (px height))
               :transform (str "rotateY(0deg) translateZ("
                               (px (/ spine-width 2)) ")")}}]
     [:div.face.back
      {:style {:background-image (img (str book "/02.jpg"))
               :width (px width)
               :height (px height)
               :background-size (str (px width) " " (px height))
               :transform (str "rotateY(180deg) translateZ("
                               (px (/ spine-width 2)) ")")}}]
     [:div.face.left
      {:style {:background-image (img (str book "/03.jpg"))
               :width (px spine-width)
               :height (px height)
               :background-size (str (px spine-width) " " (px height))
               :left (px (- (/ width 2) (/ spine-width 2)))
               :transform (str "rotateY(-90deg) translateZ("
                               (px (/ width 2)) ")")}}]
     [:div.face.right
      {:style {:background-image (img "pages/01.jpg")
               :width (px spine-width)
               :height (px height)
               :background-size (str (px spine-width) " " (px height))
               :left (px (- (/ width 2) (/ spine-width 2)))
               :transform (str "rotateY(90deg) translateZ("
                               (px (/ width 2)) ")")}}]
     [:div.face.top
      {:style {:background-image (img "pages/02-rot.jpg")
               :width (px width)
               :height (px spine-width)
               :background-size (str (px width) " " (px spine-width))
               :top (px (- (/ height 2) (/ spine-width 2)))
               :transform (str "rotateX(90deg) translateZ("
                               (px (/ height 2)) ")")}}]
     [:div.face.bottom
      {:style {:background-image (img "pages/03-rot.jpg")
               :width (px width)
               :height (px spine-width)
               :background-size (str (px width) " " (px spine-width))
               :top (px (- (/ height 2) (/ spine-width 2)))
               :transform (str "rotateX(-90deg) translateZ("
                               (px (/ height 2)) ")")}}]]
    ))

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
