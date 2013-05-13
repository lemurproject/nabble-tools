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

; Dates pertaining to our crawl period
(def start-date (clj-time/date-time 2011 12 31))
(def end-date (clj-time/date-time 2012 06 30))

(def *topic-links-output-file* "/bos/tmp19/spalakod/clueweb12pp/jobs/nabble/nabble-index-pages-processing/topic-links-list.txt")
(def *error-warcs-file* "/bos/tmp19/spalakod/clueweb12pp/jobs/nabble/nabble-index-pages-processing/wrong-warcs-list.txt")

(def *topic-links-handle* (io/writer *topic-links-output-file*))
(def *error-warcs-handle* (io/writer *error-warcs-file*))

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
  [page-resource page-record]
  (letfn [(isolate-epoch
            [js-string]
            (last (re-find #"new Date\(([0-9]*)\)" js-string)))

          (epoch-to-datetime
            [epoch-str]
            (try (time-coerce/from-long (. Long parseLong epoch-str))
                 (catch Exception e (do (.write *error-warcs-handle* (:target-uri-str page-record))
                                        (.write *error-warcs-handle* "\n")
                                        nil))))]
    (filter
     identity
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
                  (html/select page-resource [:a :script])))))))))

(defn handle-nabble-subforum
  "Args:
    page-stream : A stream to a nabble subforum index page
   Returns:
    list of topic links on the page"
  [page-record]
  (let [page-resource (html/html-resource (:payload-stream page-record))
        links-on-page (get-page-topic-links page-resource)
        dates-on-page (get-page-dates page-resource page-record)]
    (if (some
         (fn [post-date] (clj-time/within? (clj-time/interval start-date end-date) post-date))
         dates-on-page)
      links-on-page
      (list))))

(defn process-nabble-warc
  "Processes one nabble warc file"
  [warc-file-name]
  (for [record (warc/get-http-records-seq (warc/get-warc-reader warc-file-name))]
    (handle-nabble-subforum record)))

(defn -main
  [& args]
  
  (with-open [out (io/writer *topic-links-output-file*)]
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
            (do (.write out record)
                (.write out "\n"))))))))
