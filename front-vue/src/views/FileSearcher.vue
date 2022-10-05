<template>
  <div class="file-searcher">
    <h1>Celeste File Searcher</h1>

    <p>
      Use this tool to find in which Celeste mod(s) a file is on GameBanana,
      based on its path in the zip.
    </p>
    <p>
      This can be useful when looking at conflict logs, or to figure out which
      mod a decal/styleground comes from!
    </p>

    <form v-on:submit="searchTriggered">
      <input
        v-model="query"
        class="search form-control"
        placeholder="Search for a file path..."
      />
    </form>

    <div class="search-options">
      <div class="form-check">
        <input
          class="form-check-input"
          type="checkbox"
          v-model="exact"
          id="exact-option"
        />
        <label class="form-check-label" for="exact-option">
          Exact matches only
        </label>
      </div>
    </div>

    <button
      class="btn btn-primary"
      v-on:click="searchTriggered"
      :disabled="loading || query.length === 0"
    >
      Search
    </button>

    <div class="loading" v-if="loading">Searching...</div>

    <div class="error" v-if="error">
      <div class="warning">
        An error occurred. Check that you uploaded a valid Celeste map.
      </div>
    </div>

    <div class="results" v-if="results !== null">
      <div v-if="truncated">
        <b>Showing first 100 results only.</b> Try searching for something more
        precise to get a full list!
      </div>

      <ul v-if="results.length > 0">
        <li v-for="result in results" v-bind:key="result.fileid">
          <b>
            <a :href="result.url" target="_blank">{{ result.name }}</a>
          </b>
          <div v-for="file in result.files" v-bind:key="file.id" class="result">
            ➡
            <span v-if="file.url !== undefined">
              <a :href="file.url" target="_blank">{{ file.name }}</a>
              <span v-if="file.description.trim().length !== 0">
                ({{ file.description }})
              </span>
            </span>
            <span v-else>[deleted file]</span>
          </div>
        </li>
      </ul>
      <div class="no-results" v-else>
        <b>No results.</b>
      </div>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import config from "../config";

const sleep = (ms) => {
  return new Promise((resolve) => setTimeout(resolve, ms));
};

const fetchResultInfo = async (result) => {
  const info = (
    await axios.get(`${config.backendUrl}/celeste/gamebanana-search`, {
      params: {
        q: `+type: ${result.itemtype} +id: ${result.itemid}`,
        full: true,
      },
    })
  ).data;

  result.name = info[0].Name;
  result.url = info[0].PageURL;

  for (const file of result.files) {
    for (const retrievedFile of info[0].Files) {
      if (retrievedFile.URL === "https://gamebanana.com/dl/" + file.id) {
        file.name = retrievedFile.Name;
        file.url = retrievedFile.URL;
        file.description = retrievedFile.Description;
      }
    }
  }

  return result;
};

const vue = {
  name: "file-searcher",
  data: () => ({
    loading: false,
    error: false,
    truncated: false,
    results: null,
    query: "",
    exact: false,
  }),
  methods: {
    setResults: async function (results) {
      // group results by mod
      const groupedResults = [];
      let current = {};
      for (const result of results) {
        if (
          result.itemtype === current.itemtype &&
          result.itemid === current.itemid
        ) {
          current.files.push({ id: result.fileid });
        } else {
          current = {
            itemtype: result.itemtype,
            itemid: result.itemid,
            files: [{ id: result.fileid }],
          };
          groupedResults.push(current);
        }
      }

      // for each result, retrieve mod name and file names
      const promises = [];
      for (const result of groupedResults) {
        // only display up to 100 results...
        if (promises.length >= 100) {
          this.truncated = true;
          break;
        }

        promises.push(fetchResultInfo(result));
      }

      this.results = await Promise.all(promises);
    },
    searchTriggered: async function (e) {
      e.preventDefault();

      if (this.query.length === 0) {
        return;
      }

      this.error = false;
      this.loading = true;
      this.truncated = false;
      this.results = null;

      try {
        for (let i = 0; i < 60; i++) {
          const result = (
            await axios.get(`${config.backendUrl}/celeste/file-search`, {
              params: {
                query: this.query.replace("\\", "/"),
                exact: this.exact,
              },
            })
          ).data;

          if (!result.pending) {
            // search is over! break out.
            await this.setResults(result);
            break;
          }

          // search is not over yet, retry in a bit!
          await sleep(1000);
        }

        if (this.results === null) {
          // we waited for a result for more than a minute, this is not normal.
          this.error = true;
        }
      } catch {
        // an unexpected error occurred!
        this.error = true;
      }

      this.loading = false;
    },
  },
};

export default vue;
</script>

<style lang="scss" scoped>
h1 {
  margin-bottom: 30px;
}

.loading {
  margin-top: 40px;
  font-size: 16pt;
}

.error {
  font-size: 18pt;
  color: #ff8000;
  margin: 20px;
}

input.search {
  margin-top: 30px;
}

.search-options {
  text-align: left;
  margin-top: 10px;
  margin-bottom: 20px;
}

@media (min-width: 576px) {
  .form-check {
    display: inline-block;
    margin-right: 20px;
  }
}

.results {
  text-align: left;
  margin: 20px 0;

  .no-results {
    text-align: center;
  }
}

.result {
  margin-left: 20px;
}
</style>
