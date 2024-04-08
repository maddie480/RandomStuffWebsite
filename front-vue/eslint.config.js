import js from "@eslint/js";
import pluginVue from "eslint-plugin-vue";
import eslintPluginPrettierRecommended from "eslint-plugin-prettier/recommended";

export default [
  js.configs.recommended,
  ...pluginVue.configs["flat/recommended"],
  eslintPluginPrettierRecommended,
];
