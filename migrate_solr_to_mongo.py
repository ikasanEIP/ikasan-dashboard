#!/usr/bin/env python3
"""
Solr to MongoDB Migration Script
Migrates all data from Solr ikasan core to MongoDB ikasan collection
"""

import requests
from requests.auth import HTTPBasicAuth
from pymongo import MongoClient
import json
import sys
import os
from datetime import datetime

# Disable proxy for localhost
os.environ['NO_PROXY'] = 'localhost,127.0.0.1'
os.environ['no_proxy'] = 'localhost,127.0.0.1'

# Configuration
SOLR_URL = "http://localhost:8983/solr/ikasan"
SOLR_AUTH = HTTPBasicAuth('ikasan', '1ka5an')
MONGO_URL = "mongodb://localhost:27017/"
MONGO_DB = "ikasan"
MONGO_COLLECTION = "ikasan"

# Batch size for processing
BATCH_SIZE = 1000

def get_total_docs():
    """Get total document count from Solr"""
    params = {
        'q': '*:*',
        'rows': 0,
        'wt': 'json'
    }
    response = requests.get(f"{SOLR_URL}/select", params=params, auth=SOLR_AUTH)
    response.raise_for_status()
    data = response.json()
    return data['response']['numFound']

def fetch_solr_docs(start, rows):
    """Fetch documents from Solr"""
    params = {
        'q': '*:*',
        'start': start,
        'rows': rows,
        'wt': 'json'
    }
    response = requests.get(f"{SOLR_URL}/select", params=params, auth=SOLR_AUTH)
    response.raise_for_status()
    data = response.json()
    return data['response']['docs']

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

def migrate():
    """Main migration function"""
    print(f"Starting Solr to MongoDB migration at {datetime.now()}")
    print(f"Source: {SOLR_URL}")
    print(f"Destination: {MONGO_URL}{MONGO_DB}.{MONGO_COLLECTION}")
    print("=" * 80)

    # Connect to MongoDB
    print("Connecting to MongoDB...")
    mongo_client = MongoClient(MONGO_URL)
    mongo_db = mongo_client[MONGO_DB]
    mongo_coll = mongo_db[MONGO_COLLECTION]

    # Get total document count
    total_docs = get_total_docs()
    print(f"Total documents in Solr: {total_docs:,}")

    # Check if MongoDB collection has data
    existing_count = mongo_coll.count_documents({})
    if existing_count > 0:
        response = input(f"WARNING: MongoDB collection already has {existing_count:,} documents. Continue? (yes/no): ")
        if response.lower() != 'yes':
            print("Migration cancelled.")
            return

    print(f"\nStarting migration in batches of {BATCH_SIZE}...")
    print("-" * 80)

    migrated_count = 0
    error_count = 0
    start = 0

    while start < total_docs:
        try:
            # Fetch batch from Solr
            print(f"Fetching documents {start:,} to {min(start + BATCH_SIZE, total_docs):,}...", end=" ")
            solr_docs = fetch_solr_docs(start, BATCH_SIZE)

            if not solr_docs:
                print("No more documents.")
                break

            # Transform and prepare for MongoDB
            mongo_docs = []
            for solr_doc in solr_docs:
                try:
                    mongo_doc = transform_solr_to_mongo(solr_doc)
                    mongo_docs.append(mongo_doc)
                except Exception as e:
                    print(f"\nError transforming document {solr_doc.get('id', 'unknown')}: {e}")
                    error_count += 1

            # Insert into MongoDB
            if mongo_docs:
                try:
                    result = mongo_coll.insert_many(mongo_docs, ordered=False)
                    inserted = len(result.inserted_ids)
                    migrated_count += inserted
                    print(f"✓ Inserted {inserted} documents")
                except Exception as e:
                    # Handle duplicate key errors (documents already exist)
                    if hasattr(e, 'details') and 'writeErrors' in e.details:
                        write_errors = e.details['writeErrors']
                        duplicates = sum(1 for err in write_errors if err['code'] == 11000)
                        inserted = len(mongo_docs) - len(write_errors)
                        migrated_count += inserted
                        print(f"⚠ Inserted {inserted} documents ({duplicates} duplicates skipped)")
                    else:
                        print(f"\n✗ Error inserting batch: {e}")
                        error_count += len(mongo_docs)

            start += BATCH_SIZE

            # Progress update
            progress = (min(start, total_docs) / total_docs) * 100
            print(f"Progress: {progress:.1f}% ({migrated_count:,}/{total_docs:,} migrated, {error_count} errors)")

        except KeyboardInterrupt:
            print("\n\nMigration interrupted by user.")
            break
        except Exception as e:
            print(f"\n✗ Error processing batch starting at {start}: {e}")
            error_count += BATCH_SIZE
            start += BATCH_SIZE

    # Final summary
    print("\n" + "=" * 80)
    print("Migration Complete!")
    print(f"Total documents in Solr: {total_docs:,}")
    print(f"Successfully migrated: {migrated_count:,}")
    print(f"Errors: {error_count}")

    # Verify MongoDB count
    final_count = mongo_coll.count_documents({})
    print(f"Documents in MongoDB: {final_count:,}")

    # Close MongoDB connection
    mongo_client.close()

    print(f"\nMigration finished at {datetime.now()}")

if __name__ == "__main__":
    try:
        migrate()
    except Exception as e:
        print(f"Fatal error: {e}", file=sys.stderr)
        sys.exit(1)
