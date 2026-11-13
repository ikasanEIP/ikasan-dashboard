/*
 * Creates the Ikasan dashboard MongoDB indexes.
 *
 * Usage (run against the dashboard database):
 *   mongosh "mongodb+srv://<user>@<cluster>/<database>" --file apply-indexes.js
 *
 * Environment variables:
 *   INDEX_DIR      directory holding the *.indexes.json / *.search.json files (default: this script's directory)
 *   ATLAS_SEARCH   set to "false" to skip the Atlas Search indexes (e.g. self-managed MongoDB)
 *   SEARCH_INDEX   Atlas Search index name (default: "default"; must match mongodb.atlas.search.index-name)
 *
 * The script is idempotent: existing identical indexes are left alone and Atlas Search index definitions are
 * updated in place.
 */
const fs = require('fs');
const path = require('path');

const indexDir = process.env.INDEX_DIR || (typeof __dirname !== 'undefined' ? __dirname : '.');
const applySearch = (process.env.ATLAS_SEARCH || 'true').toLowerCase() !== 'false';
const searchIndexName = process.env.SEARCH_INDEX || 'default';

const files = fs.readdirSync(indexDir).sort();
let failures = 0;

// 1. Standard (B-tree) indexes: each file is a createIndexes command
files.filter(f => f.endsWith('.indexes.json')).forEach(file => {
    const command = EJSON.parse(fs.readFileSync(path.join(indexDir, file), 'utf8'));
    try {
        const result = db.runCommand(command);
        if (result.ok !== 1) {
            throw new Error(EJSON.stringify(result));
        }
        print(`[${command.createIndexes}] indexes: ${result.numIndexesBefore} -> ${result.numIndexesAfter}` +
            (result.note ? ` (${result.note})` : ''));
    } catch (e) {
        failures++;
        print(`[${command.createIndexes}] FAILED to create indexes from ${file}: ${e.message}`);
    }
});

// 2. Atlas Search indexes: each file is an index definition; the collection is the file name prefix
if (applySearch) {
    files.filter(f => f.endsWith('.search.json')).forEach(file => {
        const collectionName = file.substring(0, file.length - '.search.json'.length);
        const definition = EJSON.parse(fs.readFileSync(path.join(indexDir, file), 'utf8'));
        const collection = db.getCollection(collectionName);
        try {
            const exists = collection.getSearchIndexes(searchIndexName).length > 0;
            if (exists) {
                collection.updateSearchIndex(searchIndexName, definition);
                print(`[${collectionName}] search index '${searchIndexName}' updated (rebuilds in the background)`);
            } else {
                collection.createSearchIndex(searchIndexName, definition);
                print(`[${collectionName}] search index '${searchIndexName}' created (builds in the background)`);
            }
        } catch (e) {
            failures++;
            print(`[${collectionName}] FAILED to apply search index from ${file}: ${e.message}` +
                ' (Atlas Search requires MongoDB Atlas or Atlas Local; set ATLAS_SEARCH=false to skip)');
        }
    });
} else {
    print('Skipping Atlas Search indexes (ATLAS_SEARCH=false)');
}

if (failures > 0) {
    print(`${failures} index file(s) failed`);
    quit(1);
}
