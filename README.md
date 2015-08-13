# sf-data

A Clojure script to transform Solforge's card data json files into something more suited for easy searching and rendering.

Solforge keeps a bunch of zip files with json data in them that describe all the cards and their abilities.  If I had to guess, I'd say that these files are used to ship card data over to the client during patches, and then transformed into some internal representation for the app itself.  In any case, we can use the data for our own purposes.

Solforge stores the records for each level of a card separately.  They look something like this:

```json
{
  "CardID": "SF5_u0001",
  "CardName": "Killer Bee",
  "Level": "1",
  "NextCardID": "SF5_u0002",
  "CardType": "Creature",
  "CardText": "When <this> deals battle damage to a creature or player, that creature or player gets Poison 1.",
  "Keywords": "Move 1",
  "Faction": "Nature",
  "CreatureType": "Insect",
  "Power": "1",
  "Health": "1",
  "Art": "SF5_U083(1)",
  "Token": "TRUE",
  "AIBaselineScore": "5",
  "Set": "set5",
  "InDraft": "yes",
  "InDraftPool2": "TRUE",
  "InDraftPool3": "TRUE"
}
```

This is fine as far as it goes, but I wanted to tweak it a little for my purposes.  In particular:

* Use the usual names for things like factions and keywords.  Uterra instead of Nature, mobility instead of move, etc.
* Group the levels of each card together so they can be searched and rendered together.
* Consolidate all the data into a single file.
* Omit information I didn't need to cut down on the total file size.
* Turn quasi-structured information (like the keyword string) into more structured information.

This script turns the above into something like

```json
{
  "cardId":"SF5_u0001",
  "levels":[
    {
      "creatureType":"insect",
      "free":false,
      "faction":"uterra",
      "health":1,
      "name":"Killer Bee",
      "level":1,
      "cardType":"creature",
      "text":"When Killer Bee deals battle damage to a creature or player, that creature or player gets Poison 1.",
      "keywords":[["mobility",1]],
      "attack":1,
      "art":"SF5_U083(1)"
    },
    {...},
    {...}
  ],
  "searchText":"uterra When Killer Bee deals battle damage to a creature or player that creature or player gets Poison 1 Killer Bee creature insect mobility When Killer Bee deals battle damage to a creature or player that creature or player gets Poison 3 Killer Bee creature insect mobility When Killer Bee deals battle damage to a creature or player that creature or player gets that much Poison Killer Bee creature insect mobility"
}
```

The `searchText` field is suitable for searching with, for example [sifter](https://www.npmjs.com/package/sifter).  It includes information about all levels of a card, so "torment" will still find Scythe of Chiron.

## Installation

Install [lein](http://leiningen.org/).

Run `lein deps`.

## Usage

Find the json data in your Solforge folder.  On my machine, it's at `C:\Program Files (x86)\Steam\steamapps\common\SolForge\data\released`.  Grab the newest zip file and extract it to a folder of your choice.

Run `lein run <DATA-FOLDER> <OUTPUT-FILE>`.  If you don't specify, the script will look for the data in a `data` folder next to the repository, and output `test.json` in the repository.

### Notes

Some of the art files referenced by the json have spaces in their name (for example, Firefist Uranti).  Depending on what you're trying to do with the art, this may or may not be a problem.  In particular, various webservers each have their own opinions on serving static files that need url-encoding.

Some of the art files have a different capitalization than is listed in the json files (for example, Kadras Colossus and Brighttusk Sower).  This may not be a problem if you're only working in a case-insensitive filesystem, but will probably bite you otherwise.

This script doesn't attempt to fix either problem, since there isn't a single obvious solution that's going to work for everyone.  Caveat coder.

## License

Solforge is owned by [Stoneblade Entertainement](http://solforgegame.com).

This code copyright © 2015

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
