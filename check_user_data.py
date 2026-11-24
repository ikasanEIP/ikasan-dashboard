#!/usr/bin/env python3
"""
Check user data in MongoDB to see what fields exist
"""

from pymongo import MongoClient
import json

MONGO_URL = "mongodb://localhost:27017/"

client = MongoClient(MONGO_URL)
db = client['ikasan']
collection = db['ikasan']

# Find a securityUser document
user_doc = collection.find_one({'type': 'securityUser'})

if user_doc:
    print("Found securityUser document:")
    print(json.dumps(user_doc, indent=2, default=str))
else:
    print("No securityUser documents found")

# Check what field names exist for securityUser
print("\n" + "="*80)
print("All securityUser documents:")
for doc in collection.find({'type': 'securityUser'}):
    print(json.dumps(doc, indent=2, default=str))

client.close()
