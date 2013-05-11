; process the nabble index pages and subforums we downloaded

(ns nabble-tools.nabble-index-pages
  (:gen-class :main true)
  (:require [clj-time.core :as clj-time]
            [clj-time.coerce :as time-coerce]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [net.cgrand.enlive-html :as html]
            [warc-clojure.core :as warc]))

(def start-date (clj-time/date-time 2011 12 31))

(def end-date (clj-time/date-time 2012 06 30))

(defn get-page-topic-links
  [page-resource]

  (filter
   (fn
     [x]
     (and x
          (re-find (re-matcher #"-tp" x))))
   (map
    (fn
      [a-tag]
      (-> a-tag
          :attrs
          :href))
    (html/select page-resource [:a]))))

(defn get-page-dates
  [page-resource]
  (letfn [(isolate-epoch
           [js-string]
           (last (re-find #"new Date\(([0-9]*)\)" js-string)))

          (epoch-to-datetime
           [epoch-str]
           (try (time-coerce/from-long (. Long parseLong epoch-str))
             (catch Exception e (do (println "FUCK UP") (throw)))))]
    (map
     epoch-to-datetime
     (map
      isolate-epoch
      (filter
       (fn
         [script-content]
         (re-find (re-matcher #"formatDateShort" script-content)))
       (flatten (map
                 (fn
                   [script]
                   (:content script))
                 (html/select page-resource [:a :script]))))))))

(defn handle-nabble-subforum
  [page-stream]
  (let [page-resource (html/html-resource page-stream)
        links-on-page (get-page-topic-links page-resource)
        dates-on-page (get-page-dates page-resource)]
    (if (some
         (fn [post-date] (clj-time/within? (clj-time/interval start-date end-date) post-date))
         dates-on-page)
      links-on-page
      (list))))

(defn process-nabble-warc
  "Processes one nabble warc file"
  [warc-file-name]
  (for [record (warc/get-http-records-seq (warc/get-warc-reader warc-file-name))]
    (handle-nabble-subforum (:payload-stream record))))

(defn -main
  [& args]
  (let [[optional [nabble-jobs-dir] banner] (cli/cli args)
        nabble-jobs-dir-handle (io/file nabble-jobs-dir)]
    (doseq [warc-file (filter
                        (fn [nabble-file]
                          (and (.endsWith (.getName nabble-file) "warc.gz")
                               (not (re-find (re-matcher
                                              #"latest"
                                              (.getAbsolutePath nabble-file))))))
                        (file-seq nabble-jobs-dir-handle))]
      (doseq [processed-records (filter (fn [record]
                                          (not (empty? record)))
                                        (process-nabble-warc
                                         (.getAbsolutePath warc-file)))]
        (doseq [record processed-records]
          (println record))))))
