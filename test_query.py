#!/usr/bin/env python3
"""
Test the exact query that MongoUserDaoImpl.getUser would run
"""

from pymongo import MongoClient
import json

MONGO_URL = "mongodb://localhost:27017/"

client = MongoClient(MONGO_URL)
db = client['ikasan']

# Check what collections exist
print("Collections in ikasan database:")
for collection_name in db.list_collection_names():
    print(f"  - {collection_name}")

print("\n" + "="*80)

# Try query on ikasan collection (where the migrated data is)
ikasan_collection = db['ikasan']
result = ikasan_collection.find_one({'name': 'admin', 'type': 'securityUser'})
print("Query on 'ikasan' collection with {name: 'admin', type: 'securityUser'}:")
if result:
    print(json.dumps(result, indent=2, default=str))
else:
    print("  No result found")

print("\n" + "="*80)

# Try query on mongoUserRecord collection (what Spring might be using by default)
if 'mongoUserRecord' in db.list_collection_names():
    mongo_user_record_collection = db['mongoUserRecord']
    result = mongo_user_record_collection.find_one({'name': 'admin', 'type': 'securityUser'})
    print("Query on 'mongoUserRecord' collection with {name: 'admin', type: 'securityUser'}:")
    if result:
        print(json.dumps(result, indent=2, default=str))
    else:
        print("  No result found")
else:
    print("'mongoUserRecord' collection does not exist")

client.close()
