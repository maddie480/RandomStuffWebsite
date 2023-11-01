import { nextTick } from "vue";
import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/",
    name: "HomePage",
    component: () => import("../views/HomePage.vue"),
  },
  {
    path: "/celeste/banana-mirror-browser",
    name: "BananaMirrorBrowser",
    component: () => import("../views/BananaMirrorBrowser.vue"),
    meta: {
      title: "Banana Mirror Browser",
    },
  },
  {
    path: "/celeste/asset-drive",
    name: "AssetBrowser",
    component: () => import("../views/AssetDriveBrowser.vue"),
    meta: {
      title: "Celeste Asset Drive Browser",
    },
  },
  {
    path: "/celeste/wipe-converter",
    name: "WipeConverter",
    component: () => import("../views/WipeConverter.vue"),
    meta: {
      title: "Celeste Wipe Converter",
    },
  },
  {
    path: "/celeste/map-tree-viewer",
    name: "MapTreeViewer",
    component: () => import("../views/MapTreeViewer.vue"),
    meta: {
      title: "Celeste Map Tree Viewer",
    },
  },
  {
    path: "/celeste/file-searcher",
    name: "FileSearcher",
    component: () => import("../views/FileSearcher.vue"),
    meta: {
      title: "Celeste File Searcher",
    },
  },
  {
    path: "/celeste/graphics-dump-browser",
    name: "GraphicsDumpBrowser",
    component: () => import("../views/GraphicsDumpBrowser.vue"),
    meta: {
      title: "Celeste Graphics Dump Browser",
    },
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import("../views/RouteNotFound.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  base: process.env.BASE_URL,
  routes,
});

const DEFAULT_TITLE = "Maddie's Random Stuff";
router.afterEach((to) => {
  // Use next tick to handle router history correctly
  // see: https://github.com/vuejs/vue-router/issues/914#issuecomment-384477609
  nextTick(() => {
    document.title = to.meta.title || DEFAULT_TITLE;
  });
});

export default router;
