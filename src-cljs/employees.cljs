(ns clocking.client.employees
  (:require [clocking.client.common :as common]
               [clojure.string :as string]
               [clojure.set :as set]
            [goog.dom :as googdom]
            [goog.events :as events]
            [goog.ui.DatePicker]
            [goog.ui.DatePicker.Events]
            [goog.date.Date]
            [goog.date.Interval]
            [clojure.browser.dom :as dom]
            [dommy.template :as template]
            [ajax.core :refer [GET POST]]))

(def userid (js/parseInt (last (clojure.string/split js/document.URL #"/"))))

(defn date-in-range [date from-date to-date]
  (let [to-date-plus-one-day (doto (.clone to-date)
                    (.add (goog.date.Interval. 0 0 1)))]
    (and
     (>= 0 (goog.date.Date.compare from-date date))
     (>= 0 (goog.date.Date.compare date to-date-plus-one-day))))
  )

(defn create-datepicker [date]
  (let [picker (new goog.ui.DatePicker)]
    (.setUseNarrowWeekdayNames picker true)
    (.setUseSimpleNavigationMenu picker true)
    (.setAllowNone picker false)
    (.setShowToday picker false)
    (.setFirstWeekday picker 0)
    (.setDate picker date)
    picker))

(def from-datepicker
  (let [date (new goog.date.Date)]
    (.setDate date 1)
    (create-datepicker date)))
(def to-datepicker (create-datepicker (new goog.date.Date)))

(defn event-row [{:keys [date clock-in clock-out]}]
  (template/node
   [:tr
    [:td (when-not (nil? date) (.format common/date-formatter date))]
    [:td (when-not (nil? clock-in) (.format common/time-formatter clock-in))]
    [:td (when-not (nil? clock-out) (.format common/time-formatter clock-out))]
    [:td (common/format-minutes (common/minutes-between clock-in clock-out))]]))

(defn employee-report [events]
  (template/node
   [:div {:class "employee-report"}
    [:a {:href (str "/api/event/" userid "/" (.format common/date-formatter-link (.getDate from-datepicker)) "/" (.format common/date-formatter-link (.getDate to-datepicker)) "/report.csv")} "Download report"]
    [:div (str "Showing hours from " (.format common/date-formatter (.getDate from-datepicker)) " to " (.format common/date-formatter (.getDate to-datepicker)) )]
    [:div {:class "total-hours"} (str "Total hours: " (common/format-minutes (common/sum-hours events)))]
    [:table [:tr [:th "Date"] [:th "Clocked in"] [:th "Clocked out" ] [:th "Sum"]]
     (map event-row events)]]))

;;todo reimplement
(defn filter-events-between [events from-date to-date]
  (filter #(date-in-range (:date %) from-date to-date ) events))

(defn refresh-employee-report-filtered [events from-date to-date]

  (dom/replace-node (googdom/getElementByClass "employee-report") (employee-report (filter-events-between events from-date to-date)))
  )

;;TODO is it possible to skip this step?
(defn handle-date-change []
  (refresh-employee-report-filtered all-events (.getDate from-datepicker) (.getDate to-datepicker)))

(defn start-page []
  (template/node
   [:div {:id "employee-app"}
    [:div {:class "datepickers"}
     [:div {:id "from-datepicker" :class "datepicker"} "From"]
     [:div {:id "to-datepicker" :class "datepicker"} "To"]]
    (employee-report (filter-events-between all-events (.getDate from-datepicker) (.getDate to-datepicker)))]))

(defn buildpage []
  (.log js/console "Starting to build page.")
  (dom/replace-node (googdom/getElement "employee-app") (start-page))
  (.render from-datepicker (googdom/getElement "from-datepicker"))
  (.render to-datepicker (googdom/getElement "to-datepicker"))
  (events/listen from-datepicker goog.ui.DatePicker.Events.CHANGE handle-date-change)
  (events/listen to-datepicker goog.ui.DatePicker.Events.CHANGE handle-date-change))

(defn set-events [events]
  (.log js/console (str "Events: " events))
  (def all-events events)

  (buildpage))

;;TODO is it ok to do def all-events here?
(defn get-events-from-server []
  (.log js/console "Getting events from server.")
  (GET (str "/api/event/" userid)
       {:handler set-events
        :error-handler error-handler}
       ))

;;TODO find better way to start different apps
(when (not (nil? (googdom/getElement "employee-app") )) (get-events-from-server))
