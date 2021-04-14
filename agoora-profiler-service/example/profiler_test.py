
"""
Example to test the gRPC service without the need the start it following these steps:

1. Create a "fake" request with NASA's meteorite data.
2. Overwrite the samples.head setting with an environment variable
3. Profile request
4. Save html to file
"""

import pandas as pd
import numpy as np
from pathlib import Path
import requests
from profiler import ProfilerServicer
import os


class Request:

    def __init__(self):
        file_name = Path('rows.csv')
        if not file_name.exists():
            data = requests.get('https://data.nasa.gov/api/views/gh4g-9sfh/rows.csv?accessType=DOWNLOAD')
            file_name.write_bytes(data.content)

        df = pd.read_csv(file_name)

        # Note: Pandas does not support dates before 1880, so we ignore these for this analysis
        df['year'] = pd.to_datetime(df['year'], errors='coerce')

        # Example: Constant variable
        df['source'] = "NASA"

        # Example: Boolean variable
        df['boolean'] = np.random.choice([True, False], df.shape[0])

        # Example: Mixed with base types
        df['mixed'] = np.random.choice([1, "A"], df.shape[0])

        # Example: Highly correlated variables
        df['reclat_city'] = df['reclat'] + np.random.normal(scale=5, size=(len(df)))

        # Example: Duplicate observations
        duplicates_to_add = pd.DataFrame(df.iloc[0:10])
        duplicates_to_add[u'name'] = duplicates_to_add[u'name'] + " copy"

        df = df.append(duplicates_to_add, ignore_index=True)

        self.json_data = df.to_json(orient='records', lines=False)
        self.id = u'1'

os.environ["samples_head"] = "3"

request = Request()
ps = ProfilerServicer()
response = ps.profile(request, None)

with open('profile.html', 'w') as f:
    f.write(response.profile)
