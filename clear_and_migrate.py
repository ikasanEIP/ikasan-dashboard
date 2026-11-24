#!/usr/bin/env python3
"""
Clear MongoDB collection and re-migrate from Solr
"""

import requests
from requests.auth import HTTPBasicAuth
from pymongo import MongoClient
import os
from datetime import datetime

os.environ['NO_PROXY'] = 'localhost,127.0.0.1'
os.environ['no_proxy'] = 'localhost,127.0.0.1'

SOLR_URL = "http://localhost:8983/solr/ikasan"
SOLR_AUTH = HTTPBasicAuth('ikasan', '1ka5an')
MONGO_URL = "mongodb://localhost:27017/"
BATCH_SIZE = 1000

def transform_solr_to_mongo(solr_doc):
    """Transform Solr document to MongoDB format"""
    mongo_doc = {}

    # Map _id field
    if 'id' in solr_doc:
        mongo_doc['_id'] = solr_doc['id']

    # Copy all other fields as-is
    for key, value in solr_doc.items():
        if key == 'id':
            continue  # Already mapped to _id
        elif key == '_version_':
            continue  # Skip Solr internal field
        else:
            mongo_doc[key] = value

    return mongo_doc

def main():
    print(f"Starting Solr to MongoDB migration at {datetime.now()}")
    print(f"Source: {SOLR_URL}")
    print(f"Destination: {MONGO_URL}ikasan.ikasan")
    print("=" * 80)

    # Connect to MongoDB
    print("Connecting to MongoDB...")
    client = MongoClient(MONGO_URL)
    db = client['ikasan']
    collection = db['ikasan']

    # Clear existing data
    existing_count = collection.count_documents({})
    if existing_count > 0:
        print(f"Clearing {existing_count:,} existing documents from MongoDB...")
        collection.delete_many({})
        print("✓ MongoDB collection cleared")

    # Get total count from Solr
    response = requests.get(
        f"{SOLR_URL}/select",
        params={'q': '*:*', 'rows': 0, 'wt': 'json'},
        auth=SOLR_AUTH
    )
    total_docs = response.json()['response']['numFound']
    print(f"Total documents to migrate from Solr: {total_docs:,}")
    print()

    # Migrate in batches
    migrated = 0
    start = 0

    while start < total_docs:
        # Fetch batch from Solr
        response = requests.get(
            f"{SOLR_URL}/select",
            params={
                'q': '*:*',
                'rows': BATCH_SIZE,
                'start': start,
                'wt': 'json'
            },
            auth=SOLR_AUTH
        )

        docs = response.json()['response']['docs']
        if not docs:
            break

        # Transform and insert
        mongo_docs = [transform_solr_to_mongo(doc) for doc in docs]

        try:
            collection.insert_many(mongo_docs, ordered=False)
            migrated += len(mongo_docs)
        except Exception as e:
            # Handle duplicate key errors
            if 'duplicate key error' in str(e):
                # Try inserting one by one
                for doc in mongo_docs:
                    try:
                        collection.insert_one(doc)
                        migrated += 1
                    except:
                        pass  # Skip duplicates
            else:
                print(f"Error inserting batch: {e}")

        start += BATCH_SIZE
        progress = (migrated / total_docs) * 100
        print(f"Progress: {migrated:,} / {total_docs:,} ({progress:.1f}%)", end='\r')

    print()
    print("=" * 80)
    print(f"Migration complete!")
    print(f"Documents migrated: {migrated:,}")
    print(f"Total in MongoDB: {collection.count_documents({}):,}")

    client.close()

if __name__ == "__main__":
    main()
