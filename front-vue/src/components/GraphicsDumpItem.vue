<template>
  <div class="col-xl-3 col-lg-4 col-md-6 col-xs-12 base">
    <div class="card">
      <div class="card-body">
        <div class="image-container">
          <img :src="imagePath" loading="lazy" />
        </div>
        <h5 class="card-title">{{ name }}</h5>
        <a
          class="btn btn-primary"
          target="_blank"
          :href="'/celeste/graphics-dump-browser/' + path"
          >Open</a
        >
        <button class="btn btn-secondary" @click="copyPath">Copy path</button>
      </div>

      <div
        :id="'success-' + path.replace('/', '-')"
        class="toast fade hide"
        style="position: fixed; bottom: 10px; right: 10px"
      >
        <div class="toast-body bg-success">
          Path "{{ path }}" copied to the clipboard!
        </div>
      </div>
      <div
        :id="'failure-' + path.replace('/', '-')"
        class="toast fade hide"
        style="position: fixed; bottom: 10px; right: 10px"
      >
        <div class="toast-body bg-danger">
          Failed to copy path "{{ path }}" to the clipboard!
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import config from "../config";

export default {
  props: {
    name: { type: String, required: true },
    path: { type: String, required: true },
  },
  data: () => ({
    expanded: false,
  }),
  computed: {
    imagePath() {
      return config.backendUrl + "/celeste/graphics-dump-browser/" + this.path;
    },
  },
  methods: {
    async copyPath() {
      const slugifiedPath = this.path.replace("/", "-");

      navigator.clipboard.writeText(this.path).then(
        function () {
          // eslint-disable-next-line no-undef
          const toastBootstrap = bootstrap.Toast.getOrCreateInstance(
            document.getElementById("success-" + slugifiedPath),
          );
          toastBootstrap.show();
        },
        function () {
          // eslint-disable-next-line no-undef
          const toastBootstrap = bootstrap.Toast.getOrCreateInstance(
            document.getElementById("failure-" + slugifiedPath),
          );
          toastBootstrap.show();
        },
      );
    },
  },
};
</script>

<style lang="scss" scoped>
.card {
  margin: 0 5px 10px 5px;
}
.base {
  margin: 0;
  padding: 0;
}

.btn-secondary {
  margin-left: 5px;
}
.image-container {
  text-align: center;
}

img {
  max-width: 100%;
  max-height: 400px;
}

.bg-success,
.bg-danger {
  color: #fff;
}

.toast {
  z-index: 100;
}

.card-title {
  margin: 10px 0;
}
</style>
