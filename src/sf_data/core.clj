(ns sf-data.core
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.data.json :as json])
  (:import [java.util.zip.ZipInputStream]))

(def test-output-name "./test.json")
(def test-folder-name "../data")

(defn relevant-file? [file-name]
  (and (not (nil? (re-find #"_Cards" file-name)))
       (nil? (re-find #"Mission" file-name))
       (nil? (re-find #"SpecialArt" file-name))))

(defn file->levels [file-name]
  (with-open [rdr (io/reader file-name)]
    ((json/read rdr) "cards")))

(defn folder->levels [folder-name]
  (->> folder-name
       (io/file)
       file-seq
       (map #(.getAbsolutePath %))
       (filter relevant-file?)
       (mapcat file->levels)))

(defn cards->file [file-name cards]
  (with-open [wrtr (io/writer file-name)]
    (json/write cards wrtr)))

(defn levels-indexed [levels]
  (reduce #(assoc %1 (%2 "CardID") %2) {} levels))

(defn root-level? [level]
  (nil? (level "PrevCardID")))

(defn end-level? [level]
  (nil? (level "NextCardID")))

(defn root-levels [levels]
  (filter root-level? levels))

(defn collect-card-levels [indexed-levels root-level]
  (loop [levels [root-level]
         level root-level]
    (if (end-level? level)
        levels
        (let [next-level (indexed-levels (level "NextCardID"))]
          (recur (conj levels next-level) next-level)))))

(defn translate-faction [faction]
  ({"Elemental" "tempys" "Nature" "uterra" "Mechanical" "alloyin" "Death" "nekrium"} faction))

(defn clean-level-text [text level-name]
  (s/replace text #"[<*](?:.*?)[>*]" #(get {"<this>" level-name "*blank*" ""} % "")))

(defn translate-keyword [keyword-and-maybe-value]
  (let [word-map {"Fast" "aggressive" "Armor" "armor" "Breakthrough" "breakthrough"
                  "Consistent" "consistent" "Defender" "defender" "Move" "mobility"
                  "RemoveWhenPlayed" "overload" "Regenerate" "regenerate"}
        groups (re-find #"([A-Za-z]+) ?(\d+)?" (s/trim keyword-and-maybe-value))
        kyword (second groups)
        value (nth groups 2)]
    (if (nil? value)
        [(word-map kyword)]
        [(word-map kyword) (Integer/parseInt value)])))

(defn clean-keywords [keywordstr]
  (if (nil? keywordstr)
      []
      (map translate-keyword
           (s/split keywordstr #", "))))

(defn clean-level [level]
  {"name" (level "CardName")
   "level" (Integer/parseInt (level "Level"))
   "keywords" (clean-keywords (level "Keywords"))
   "free" (= "0" (level "ActionsToPlay"))
   "text" (clean-level-text (get level "CardText" "") (level "CardName"))
   "faction" (translate-faction (level "Faction"))
   "attack" (when-let [attack (level "Power")] (Integer/parseInt attack))
   "health" (when-let [health (level "Health")] (Integer/parseInt health))
   "art" (level "Art")
   "cardType" (s/lower-case (level "CardType"))
   "creatureType" (when-not (nil? (level "CreatureType")) (s/lower-case (level "CreatureType")))})

(defn level-search-text [cleaned-level]
  (str (s/replace (cleaned-level "text") #"[^a-zA-Z0-9 \n]" "")
       " "
       (cleaned-level "name")
       " "
       (cleaned-level "cardType")
       " "
       (cleaned-level "creatureType")
       " "
       (s/join " " (map first (cleaned-level "keywords")))))

(defn card-levels->card [levels]
  (let [cleaned-levels (map clean-level levels)]
    {"cardId" (first (map #(% "CardID") levels))
     "levels" cleaned-levels
     "searchText" (str
        ((first cleaned-levels) "faction")
        " "
        (s/join " " (map level-search-text cleaned-levels)))}))

(defn -main [& args]
  (let [levels (folder->levels test-folder-name)
        indexed-levels (levels-indexed levels)]
    (->> levels
         root-levels
         (map (partial collect-card-levels indexed-levels))
         (map card-levels->card)
         (cards->file test-output-name))))

; (defn -main [& args]
;   (let [levels (folder->levels test-folder-name)
;         indexed-levels (levels-indexed levels)]
;     (->> levels
;          root-levels
;          (map (partial collect-card-levels indexed-levels))
;          (map card-levels->card)
;          (map #(get % "cardId"))
;          (group-by count)
;          println)))
