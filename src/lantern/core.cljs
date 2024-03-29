(ns lantern.core
    (:require
     [reagent.core :as r]
     [cemerick.url :as url]
     [clojure.string :as string]
     [cljs.reader :refer [read-string]]))

(def book-details (atom {}))

(defn ^:dynamic book-id [book]
  (str "book" book))

(defn ^:dynamic cont-id [book]
  (str "book" book "cont"))

(defn ^:dynamic page-id [book page]
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

(defn ^:dynamic image-url [string]
  (str "https://quimby.gnus.org/circus/lanterne/" string))

(defn img [images image]
  (let [url (image-url image)]
    ;; Save images so that we can later check that they loaded.
    (when images
      (swap! images conj {url :new}))
    (str "url('" url "')")))

(defn tz [z]
  (str "translateZ(" z "px)"))

(defn ty [z]
  (str "translateY(" z "px)"))

(defn tx [z]
  (str "translateX(" z "px)"))

(defn rx [x]
  (str "rotateX(" x "deg)"))

(defn ry [y]
  (str "rotateY(" y "deg)"))

(defn rz [z]
  (str "rotateZ(" z "deg)"))

(defn trans [& elems]
  (reduce str (interpose " " elems)))

(defn spin-degrees []
  (* (Math.round (rand 10)) 360))

(defn make-spinner [name]
  ;; The idea here is that we want to spint along all three axis --
  ;; smoothy, but at random speeds (so that X is slower or faster
  ;; than Y etc), so that each book spins uniquely.  But smoothly.
  (str "@keyframes spinner-" name " { "
       " 0% { transform: "
       (trans (ry 0)
              (rx 0)
              (rz 0))
       "; } "
       " 100% { transform: "
       (trans (ry (spin-degrees))
              (rx (spin-degrees))
              (rz (spin-degrees)))
       ";}}"))

(defmulti read-book-state #'identity)

(defmethod read-book-state :spinning [_ book id state]
  ;; Set the current animation 3d transform so we have something to
  ;; transition from.
  (set! (.-transform (find-style id)) (js/window.getRotation (find-node id)))
  (set! (.-animationName (find-style id)) "")
  (reset! state :front)
  ;; Chrome needs to do a reflow before adding the transition class.
  (js/setTimeout #(add-class id "see-front") 10))

(defmethod read-book-state :front [_ book id state]
  (prn book id state)
  (reset! state :back)
  (remove-class id "see-front")
  (add-class id "see-back"))

(defmethod read-book-state :back [_ book id state]
  (reset! state :open-1)
  (remove-class id "hide-pages")
  (remove-class id "see-back")
  (add-class id "open-1")
  (js/setTimeout #(add-class (cont-id book) "container-spinning") 1000))

(defmethod read-book-state :open-1 [_ book id state]
  (reset! state :open-2)
  (remove-class id "open-1")
  (add-class id "open-2"))

(defmethod read-book-state :open-2 [_ book id state]
  (reset! state :open-3)
  (remove-class id "open-2")
  (add-class id "open-3"))

(defmethod read-book-state :open-3 [_ book id state]
  (reset! state :open-4)
  (remove-class id "open-3")
  (add-class id "open-4"))

(defmethod read-book-state :open-4 [_ book id state]
  (reset! state :spinning)
  (remove-class id "open-4")
  (add-class id "normal")
  (add-class id "closing")
  (js/setTimeout
   (fn []
     (when (= (.-animationName (find-style id)) "")
       (set! (.-animationName (find-style id)) (str "spinner-" (book-id book))))
     (remove-class id "normal"))
   1000)
  (js/setTimeout (fn []
                   (add-class id "hide-pages")
                   (remove-class id "closing"))
                 5000))

(defn read-book [book id state]
  (read-book-state @state book id state))

(declare make-book)
(declare wait-for-images)

(def current-book (atom nil))

(defn display-details [book]
  (when (not (= book @current-book))
    (reset! current-book book)
    (let [details (get @book-details book)]
      (binding [image-url (fn [string]
                            (str "https://quimby.gnus.org/circus/lanterne/tiny/"
                                 string))
                book-id #(str "tiny-book" %)
                cont-id #(str "tiny-book" % "cont")
                page-id #(str "tiny-book" %2 %2)]
        (let [id (cont-id book)]
          (r/render [:table>tbody>tr
                     [:td.thumbnail
                      [:div
                       (wait-for-images (make-book :book book
                                                   :spine-width (nth details 1)
                                                   :shrink 20
                                                   :pages 0
                                                   :on-click false
                                                   :opacity 0)
                                        #(add-class id "fade-in-fast"))]]
                     [:td.details
                      [:div
                       [:div
                        [:div (nth details 3)]
                        [:div (nth details 4)]
                        [:div (nth details 2)]]]]
                     [:td.number
                      (str "L " (nth details 0))]]
                    (find-node "current-book"))
          (prn book))))))

(defn make-book [& {:keys [book spine-width spines-only on-click shrink pages
                           opacity]
                    :or {shrink 4
                         pages 8
                         opacity 1
                         on-click 'default}}]
  (let [images (atom {})
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
     [:div.book-container {:id (cont-id book)
                           :style {:opacity opacity}}
      [:div.book
       {:id id
        :on-mouse-over (fn []
                         (when (or (= @state :spine)
                                   (= @state :put-back))
                           (display-details book)))
        :on-click (when on-click
                    (if (= on-click 'default)
                      #(read-book book id state)
                      (on-click state)))
        :style (if spines-only
                 {:animation-duration (str (+ (rand 10) 50) "s")
                  :transform (trans (tz 0)
                                    (tx (/ spine-width shrink))
                                    (ry 90)
                                    (rx 5)
                                    (str " scale(0.5)")
                                    (str " scaleZ(0.5)"))}
                 {:animation-duration (str (+ (rand 10) 50) "s")
                  :transform-style "preserve-3d"
                  :width (px width)
                  :height (px height)
                  :animation-name (str "spinner-" (book-id book))})}
       ;; The spinner animation keyframes.
       [:style (make-spinner (book-id book))]
       ;; The interior pages.
       (doall
        (map (fn [page]
               (let [pic (Math.floor (/ page 2))]
                 (if (odd? page)
                   [:div.face.page
                    {:key (str "page" page)
                     :style
                     {:width (px width)
                      :height (px height)
                      :transform (trans (tz (- (/ spine-width 2)
                                               (+ (/ page 10) 0.1)))
                                        (rx 0))}}
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
                   [:div.face.page
                    {:key (str "page" page)
                     :style {:width (px width)
                             :height (px height)
                             :transform (trans (rz 180)
                                               (tz (- (/ spine-width 2)
                                                      (+ (/ page 10) 0.1)))
                                               (rx 180))}}
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
             (reverse (range 0 pages))))
       [:div.face
        {:style {:width (px width)
                 :height (px height)
                 :transform (trans (ry 0) (tz (/ spine-width 2)))}}
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
                 :transform (trans (ry 180) (tz (/ spine-width 2)))}
         :id (page-id book "back")
         :on-click oc}]
       [:div.face.left
        {:style {:background-image (img images (str book "/03.jpg"))
                 :width (px spine-width)
                 :height (px height)
                 :background-size (str (px spine-width) " " (px height))
                 :left (px (- (/ width 2) (/ spine-width 2)))
                 :transform (trans (ry -90) (tz (/ width 2)))}
         :id (page-id book "left")
         :on-click oc}]
       [:div.face.right
        {:style {image-property (simg images (str "pages/" ear "/01.jpg"))
                 :width (px spine-width)
                 :height (px height)
                 :background-size (str (px spine-width) " " (px height))
                 :left (px (- (/ width 2) (/ spine-width 2)))
                 :transform (trans (ry 90) (tz (/ width 2)))}
         :id (page-id book "right")
         :on-click oc}]
       [:div.face.top
        {:style {image-property (simg images (str "pages/" ear "/02.jpg"))
                 :width (px width)
                 :height (px spine-width)
                 :background-size (str (px width) " " (px spine-width))
                 :top (px (- (/ height 2) (/ spine-width 2)))
                 :transform (trans (rx 90) (tz (/ height 2)))}
         :id (page-id book "top")
         :on-click oc}]
       [:div.face.bottom
        {:style {image-property (img images (str "pages/" ear "/03.jpg"))
                 :width (px width)
                 :height (px spine-width)
                 :background-size (str (px width) " " (px spine-width))
                 :top (px (- (/ height 2) (/ spine-width 2)))
                 :transform (trans (rx -90) (tz (/ height 2)))}
         :id (page-id book "bottom")
         :on-click oc}]]])))

(defn wait-for-images [[images id html] & [callback]]
  (let [loaded (fn [node url]
                 (swap! images conj {url :loaded})
                 (when (every? (fn [[_ status]]
                                 (= status :loaded))
                               @images)
                   (if callback
                     ;; Call the provided callback.
                     (callback)
                     ;; The default callback.
                     (add-class id "fade-in"))))]
    [:div {:style {:transform-style "preserve-3d"}
           :key (first (first @images))}
     html
     [:div {:style {:display "none"}}
      (map (fn [[url state]]
              [:img {:key url
                     :on-load #(loaded %1 url)
                     :src url}])
           @images)]]))

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
(def prev-click (atom nil))
(defn z-index [id]
  (set! (.-zIndex (find-style id)) (swap! book-z-index inc)))

(defn take-out-library-book [id book width state]
  (prn "taking out" id state)
  (z-index id)
  (when (not (= book @prev-click))
    (reset! prev-click book)
    (set! (.-transform (find-style (cont-id book)))
          (trans (tz (* 20 (swap! book-z-index inc))))))
  (cond
    (= @state :put-back)
    (do
      (reset! state :front)
      (remove-class (book-id book) (str "put-back-book-" book))
      (remove-class (cont-id book) "put-back-cont")
      (add-class (book-id book) "see-front"))
    (= @state :spine)
    (let [done (atom false)
          start (.getTime (js/Date.))]
      (add-class (book-id book) "take-out-slide")
      ;; If we don't get all the images, roll back.
      (js/setTimeout
       (fn []
         (when (not @done)
           (reset! done true)
           (remove-class (book-id book) "take-out-slide")
           (reset! state :spine)))
       5000)
      (z-index id)
      (let [images (atom {})]
        (doseq [class '("front" "back" "right" "top" "bottom"
                        "page0" "page1" "page2" "page3" "page4" "page5"
                        "page6" "page7")]
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
         (find-node (str "preload-" book)))))
    (= @state :spinning)
    (do
      ;; Set the current animation 3d transform so we have something to
      ;; transition from.
      (let [node (find-node (book-id book))
            style (.-style node)]
        (set! (.-transform style) (js/window.getRotation node))
        (set! (.-animationName style) "")
        ;; Chrome needs to do a reflow before adding the transition class.
        (js/setTimeout
         (fn []
           (add-class (book-id book) "put-back-book")
           (add-class (cont-id book) "put-back-cont")
           (set! (.-transform (find-style (cont-id book)))
                 (trans (tz 0)))
           (js/setTimeout
            (fn []
              (remove-class (book-id book) "put-back-book")
              (remove-class (cont-id book) "container-spinning")
              (add-class (book-id book) (str "put-back-book-" book))
              (reset! state :put-back))
            3000))
         10)))
    true (read-book book (book-id book) state)))

(def library-sort (r/atom :numeric))

(defn author [name]
  ;; Sort by last name.
  (reduce str (interpose " " (reverse (string/split name " ")))))

(defn make-library [books]
  (let [shrink 8]
    [:div.library
     (map (fn [[book width :as details]]
            (swap! book-details assoc book details)
            (let [id (str "library-book-" book)
                  [images book-id html]
                  (make-book :book book
                             :spine-width width
                             :spines-only true
                             :on-click (fn [state]
                                         (fn [e]
                                           (take-out-library-book
                                            id book width state)))
                             :shrink 4
                             :pages 8)]
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
          (sort (if (= @library-sort :numeric)
                  #(compare (read-string (first %1))
                            (read-string (first %2)))
                  ;; Sort by author name.
                  #(compare (author (nth %1 3)) (author (nth %2 3))))
                books))]))

(defn toggle-sort []
  (reset! library-sort
          (if (= @library-sort :numeric)
            :author
            :numeric)))

(defn library []
  (let [bs (js->clj js/books)]
    [:div
     [:h2 "Library"]
     [:div.sort {:on-click #(toggle-sort)}
      [:img {:src (image-url "lantern.png")
             :width (px 40)
             :title "Toggle Sort"}]]
     (make-library bs)
     [:div#load-images (map (fn [[book]]
                              [:div {:id (str "preload-" book)
                                     :key (str "preload-" book)}])
                            bs)]
     [:div#current-book [:table>tbody>tr>td "Lanterne-bøkene"]]]))

;; -------------------------
;; Initialize app

(defmulti current-page #'identity)

(defmethod current-page :library []
  [library])

;; http://localhost:3449/index.html?page=spinning
(defmethod current-page :spinning []
  (let [bs (js->clj js/books)]
    [:div
     [:h2 "Lanterne-bøkene"]
     [:div {:style {:transform-style "preserve-3d"}}
      (map
       (fn [index]
         (let [details (nth bs index)
               book (nth details 0)]
           (wait-for-images (make-book :book book
                                       :spine-width (nth details 1)
                                       :opacity 0
                                       :on-click false)
                            #(add-class (str (book-id book) "cont")
                                        "visible"))))
       (take 3 (repeatedly #(int (rand (count bs))))))]]))

;; http://localhost:3449/index.html?page=book&book=2
(defmethod current-page :book []
  (let [bs (js->clj js/books)
        params (:query (url/url (-> js/window .-location .-href)))
        book (nth bs (first (first
                             (filter #(= (get params "book")
                                         (first (first (rest %))))
                                     (map-indexed vector bs)))))]
    [:div
     [:h2 "Lanterne-bøkene"]
     [:div.sort {:on-click #(r/render [library] (find-node "app"))}
      [:img {:src (image-url "lantern.png")
             :width (px 40)
             :title "Show Library"}]]
     [:div {:style {:transform-style "preserve-3d"}
            :class "single-book"}
      (wait-for-images (make-book :book (nth book 0)
                                  :spine-width (nth book 1)
                                  :opacity 0))]]))

(defn make-current-page []
  (let [params (:query (url/url (-> js/window .-location .-href)))]
    (if (get params "page")
      (current-page (keyword (get params "page")))
      (current-page :library))))

(defn mount-root []
  (r/render (make-current-page) (find-node "app")))

(defn init! []
  (mount-root))
