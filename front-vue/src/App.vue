<template>
  <div :class="'month-' + (new Date().getMonth() + 1)">
    <nav
      id="nav"
      class="navbar navbar-expand navbar-light bg-light border-bottom shadow-sm"
    >
      <div class="container-fluid">
        <h5 class="navbar-brand m-0">
          <a href="/"
            ><img src="/img/maddie-avatar.png" /> Maddie's Random Stuff</a
          >
        </h5>

        <ul class="navbar-nav">
          <li class="nav-item dropdown">
            <a
              id="celesteDropdown"
              :class="
                'nav-link dropdown-toggle ' +
                ($route.path.startsWith('/celeste') ? 'active' : '')
              "
              href="#"
              role="button"
              data-bs-toggle="dropdown"
              data-bs-target="#celesteDropdown"
              aria-expanded="false"
              >Celeste</a
            >
            <ul
              class="dropdown-menu dropdown-menu-sm-end"
              aria-labelledby="celesteDropdown"
            >
              <li>
                <a
                  :class="
                    'dropdown-item ' +
                    ($route.path === '/celeste/asset-drive' ? 'active' : '')
                  "
                  href="/celeste/asset-drive"
                  >Asset Drive Browser</a
                >
              </li>
              <li>
                <a
                  :class="
                    'dropdown-item ' +
                    ($route.path === '/celeste/banana-mirror-browser'
                      ? 'active'
                      : '')
                  "
                  href="/celeste/banana-mirror-browser"
                  >Banana Mirror Browser</a
                >
              </li>
              <li>
                <a
                  class="dropdown-item"
                  href="/celeste/news-network-subscription"
                  >#celeste_news_network Subscription</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/collab-contest-list"
                  >Collab & Contest List</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/custom-entity-catalog"
                  >Custom Entity Catalog</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/direct-link-service"
                  >Direct Link service</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/everest-yaml-validator"
                  >everest.yaml validator</a
                >
              </li>
              <li>
                <a
                  :class="
                    'dropdown-item ' +
                    ($route.path === '/celeste/file-searcher' ? 'active' : '')
                  "
                  href="/celeste/file-searcher"
                  >File Searcher</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/font-generator"
                  >Font Generator</a
                >
              </li>
              <li>
                <a
                  :class="
                    'dropdown-item ' +
                    ($route.path === '/celeste/graphics-dump-browser'
                      ? 'active'
                      : '')
                  "
                  href="/celeste/graphics-dump-browser"
                  >Graphics Dump Browser</a
                >
              </li>
              <li>
                <a
                  class="dropdown-item"
                  href="/lua-cutscenes-documentation/index.html"
                  target="_blank"
                  >Lua Cutscenes Documentation</a
                >
              </li>
              <li>
                <a
                  :class="
                    'dropdown-item ' +
                    ($route.path === '/celeste/map-tree-viewer' ? 'active' : '')
                  "
                  href="/celeste/map-tree-viewer"
                  >Map Tree Viewer</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/mod-structure-verifier"
                  >Mod Structure Verifier</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/olympus-news"
                  >Olympus News</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/update-checker-status"
                  >Update Checker status</a
                >
              </li>
              <li>
                <a
                  :class="
                    'dropdown-item ' +
                    ($route.path === '/celeste/wipe-converter' ? 'active' : '')
                  "
                  href="/celeste/wipe-converter"
                  >Wipe Converter</a
                >
              </li>
            </ul>
          </li>
          <li class="nav-item">
            <a class="nav-link" href="/discord-bots">Discord Bots</a>
          </li>
          <li class="nav-item dropdown">
            <a
              id="linksDropdown"
              class="nav-link dropdown-toggle"
              href="#"
              role="button"
              data-bs-toggle="dropdown"
              aria-expanded="false"
              >Links</a
            >
            <ul
              class="dropdown-menu dropdown-menu-end"
              aria-labelledby="linksDropdown"
            >
              <li><h6 class="dropdown-header">External Profiles</h6></li>
              <li>
                <a
                  class="dropdown-item"
                  href="https://gamebanana.com/members/1698143"
                  target="_blank"
                  >&#x1F34C;&nbsp;&nbsp;GameBanana &#x2013; Celeste Mods</a
                >
              </li>
              <li>
                <a
                  class="dropdown-item"
                  href="https://github.com/maddie480"
                  target="_blank"
                  >&#x1F4BB;&nbsp;&nbsp;GitHub &#x2013; Source Code</a
                >
              </li>
              <li><hr class="dropdown-divider" /></li>
              <li>
                <h6 class="dropdown-header">Les Navets Jouables (in French)</h6>
              </li>
              <li>
                <a
                  class="dropdown-item"
                  href="https://github.com/maddie480/BazarLNJ"
                  target="_blank"
                  >&#x1F50E;&nbsp;&nbsp;Bazar LNJ &#x2013; Game Investigation</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/radio-lnj"
                  >&#x1F4FB;&nbsp;&nbsp;Radio LNJ &#x2013; Game Music</a
                >
              </li>
              <li><hr class="dropdown-divider" /></li>
              <li>
                <h6 class="dropdown-header">QUEST by Laupok (in French)</h6>
              </li>
              <li>
                <a class="dropdown-item" href="/quest/mods"
                  >&#x2694;&#xFE0F;&nbsp;&nbsp;Quest &#x2013; Game Mods</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/quest/tools"
                  >&#x1F527;&#xFE0F;&nbsp;&nbsp;Quest &#x2013; Modding Tools</a
                >
              </li>
              <li><hr class="dropdown-divider" /></li>
              <li><h6 class="dropdown-header">Mirrored Stuff</h6></li>
              <li>
                <a
                  class="dropdown-item"
                  href="/static/celeste-fmod-project.zip"
                  target="_blank"
                  >&#x1F3A7;&nbsp;&nbsp;Celeste FMOD Project</a
                >
              </li>
              <li>
                <a
                  class="dropdown-item"
                  href="/static/unicode-mirror/emoji/charts/index.html"
                  target="_blank"
                  >&#x1F604;&nbsp;&nbsp;Unicode Emoji Charts</a
                >
              </li>
            </ul>
          </li>
        </ul>
      </div>
    </nav>

    <div id="app" class="container">
      <router-view />
    </div>
  </div>
</template>

<script>
const vue = {
  name: "app",
};

export default vue;
</script>

<style lang="scss">
#app {
  text-align: center;
}
</style>
