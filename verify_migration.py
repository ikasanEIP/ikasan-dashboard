#!/usr/bin/env python3
"""
Verify Solr to MongoDB migration completion
"""

import requests
from requests.auth import HTTPBasicAuth
from pymongo import MongoClient
import os

os.environ['NO_PROXY'] = 'localhost,127.0.0.1'

SOLR_URL = "http://localhost:8983/solr/ikasan"
SOLR_AUTH = HTTPBasicAuth('ikasan', '1ka5an')
MONGO_URL = "mongodb://localhost:27017/"

def main():
    print("Verifying Solr to MongoDB Migration")
    print("=" * 80)

    # Get total counts
    solr_response = requests.get(
        f"{SOLR_URL}/select",
        params={'q': '*:*', 'rows': 0, 'wt': 'json'},
        auth=SOLR_AUTH
    )
    solr_total = solr_response.json()['response']['numFound']

    client = MongoClient(MONGO_URL)
    db = client['ikasan']
    collection = db['ikasan']
    mongo_total = collection.count_documents({})

    print(f"\nTotal Documents:")
    print(f"  Solr:    {solr_total:,}")
    print(f"  MongoDB: {mongo_total:,}")
    print(f"  Match:   {'✓ YES' if solr_total == mongo_total else '✗ NO'}")

    # Check specific types that were missing
    missing_types = [
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
        'securityRole',
        'securityUser'
    ]

    print(f"\nVerifying Previously Missing Types:")
    print("-" * 80)

    all_match = True
    for type_name in missing_types:
        solr_count_response = requests.get(
            f"{SOLR_URL}/select",
            params={'q': f'type:{type_name}', 'rows': 0, 'wt': 'json'},
            auth=SOLR_AUTH
        )
        solr_count = solr_count_response.json()['response']['numFound']

        mongo_count = collection.count_documents({'type': type_name})

        match = solr_count == mongo_count
        if not match:
            all_match = False

        status = '✓' if match else '✗'
        print(f"  {status} {type_name:40s} Solr: {solr_count:6d}  MongoDB: {mongo_count:6d}")

    print("\n" + "=" * 80)
    if solr_total == mongo_total and all_match:
        print("✓ Migration verified successfully! All documents match.")
    else:
        print("✗ Migration incomplete. Some mismatches found.")

    client.close()

if __name__ == "__main__":
    main()
