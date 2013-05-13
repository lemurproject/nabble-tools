;;;; This is to process the urls emitted from the index pages of the
;;;; nabble crawl

(ns nabble-tools.process-topic-urls
  (:gen-class :main :true)
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]))

(defn handle-links
  [link1 link2]
  (do
    (println link1 link2)
    (cond
     (= link1 link2) (list link1)
     (and (< (count link1) (count link2))
        (re-find (re-pattern (string/replace link1 #"html" ""))
                 (string/replace link2 #"html" ""))) (list link1)
                 :else (list link1 link2))))

(defn -main
  [& args]
  (let [[optional [topic-links-file] banner] (cli/cli args)]
    (with-open [in (io/reader topic-links-file)]
      (doseq  [links (map (fn [links] (apply handle-links links))
                          (map vector (line-seq in) (line-seq in)))]
        (doseq [link links]
          (println link))))))
