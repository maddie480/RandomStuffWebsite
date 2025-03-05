import js from "@eslint/js";
import pluginVue from "eslint-plugin-vue";
import eslintPluginPrettierRecommended from "eslint-plugin-prettier/recommended";
import globals from "globals";

export default [
  js.configs.recommended,
  ...pluginVue.configs["flat/recommended"],
  eslintPluginPrettierRecommended,
  {
    languageOptions: {
      globals: {
        ...globals.browser,
      },
    },
  },
];
