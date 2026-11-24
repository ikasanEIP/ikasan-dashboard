#!/usr/bin/env python3
"""
Migrate missing documents from Solr to MongoDB
"""

import requests
from requests.auth import HTTPBasicAuth
from pymongo import MongoClient
import os
import sys

os.environ['NO_PROXY'] = 'localhost,127.0.0.1'

SOLR_URL = "http://localhost:8983/solr/ikasan"
SOLR_AUTH = HTTPBasicAuth('ikasan', '1ka5an')
MONGO_URL = "mongodb://localhost:27017/"

# Types that are missing documents
MISSING_TYPES = [
    'contextVisualisationLayout',
    'dashboardPlatformSetup',
    'fileEventDrivenJob',
    'globalEventJob',
    'internalEventDrivenJob',
    'internalEventDrivenJobTemplate',
    'jobLockCache',
    'jockLockCache',
    'quartzScheduleDrivenJob',
    'scheduledContext',
    'securityPolicy',
    'securityPrincipal',
    'securityRole'
]

def migrate_type(type_name, collection):
    """Migrate all documents of a specific type"""
    print(f"\nMigrating type: {type_name}")

    # Get count from Solr
    count_response = requests.get(
        f"{SOLR_URL}/select",
        params={'q': f'type:{type_name}', 'rows': 0, 'wt': 'json'},
        auth=SOLR_AUTH
    )
    total = count_response.json()['response']['numFound']
    print(f"  Found {total} documents in Solr")

    if total == 0:
        return 0

    # Fetch all documents
    response = requests.get(
        f"{SOLR_URL}/select",
        params={'q': f'type:{type_name}', 'rows': total, 'wt': 'json'},
        auth=SOLR_AUTH
    )

    docs = response.json()['response']['docs']

    # Transform and insert
    migrated = 0
    for solr_doc in docs:
        mongo_doc = {'_id': solr_doc['id']}
        for key, value in solr_doc.items():
            if key == 'id':
                continue
            elif key == '_version_':
                continue
            else:
                mongo_doc[key] = value

        try:
            result = collection.replace_one({'_id': mongo_doc['_id']}, mongo_doc, upsert=True)
            migrated += 1
        except Exception as e:
            print(f"  Error inserting {mongo_doc['_id']}: {e}")

    print(f"  ✓ Migrated {migrated} documents")
    return migrated

def main():
    print("Migrating missing documents from Solr to MongoDB")
    print("=" * 80)

    # Connect to MongoDB
    client = MongoClient(MONGO_URL)
    db = client['ikasan']
    collection = db['ikasan']

    total_migrated = 0

    for type_name in MISSING_TYPES:
        migrated = migrate_type(type_name, collection)
        total_migrated += migrated

    # Also check for missing error documents
    print(f"\nMigrating missing error documents...")
    print("  Checking for differences...")

    # Get all error IDs from Solr (just check if there are more recent ones)
    recent_response = requests.get(
        f"{SOLR_URL}/select",
        params={'q': 'type:error', 'rows': 200, 'sort': 'timestamp desc', 'wt': 'json'},
        auth=SOLR_AUTH
    )

    recent_docs = recent_response.json()['response']['docs']
    error_migrated = 0

    for solr_doc in recent_docs:
        # Check if exists in MongoDB
        if collection.count_documents({'_id': solr_doc['id']}) == 0:
            mongo_doc = {'_id': solr_doc['id']}
            for key, value in solr_doc.items():
                if key == 'id':
                    continue
                elif key == '_version_':
                    continue
                else:
                    mongo_doc[key] = value

            try:
                collection.insert_one(mongo_doc)
                error_migrated += 1
            except Exception as e:
                print(f"  Error inserting {mongo_doc['_id']}: {e}")

    print(f"  ✓ Migrated {error_migrated} missing error documents")
    total_migrated += error_migrated

    print("\n" + "=" * 80)
    print(f"Migration complete! Total documents migrated: {total_migrated}")

    # Final count
    final_count = collection.count_documents({})
    print(f"Total documents in MongoDB: {final_count:,}")

    client.close()

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"Fatal error: {e}", file=sys.stderr)
        sys.exit(1)
