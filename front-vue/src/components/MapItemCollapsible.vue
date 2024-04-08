<template>
  <div v-if="shown" class="collapsible">
    <div class="line">
      <span
        :class="
          'tree-item ' +
          (item.children.length !== 0 ? 'clickable' : '') +
          ' ' +
          (highlighted ? 'highlight' : '')
        "
        @click="expandOrCollapse"
      >
        <span v-if="item.children.length === 0" class="arrow">–</span>
        <span v-else>
          <img
            :class="'arrow pointer dark' + (expanded ? '' : ' collapsed')"
            src="/img/arrow-white.svg"
          />
          <img
            :class="'arrow pointer light' + (expanded ? '' : ' collapsed')"
            src="/img/arrow-black.svg"
          />
        </span>

        <span class="item-name">{{ item.name }}</span>
      </span>
    </div>
    <div class="attributes">
      <span
        v-for="(attribute, index) in orderedAttributes"
        :key="attribute.key"
        class="attribute"
      >
        {{ attribute.key }}:
        <span class="att-value">
          <pre
            v-if="
              ['solids', 'bg'].includes(item.name) &&
              attribute.key === 'innerText'
            "
            >{{ attribute.value }}</pre
          >
          <span v-else>{{ attribute.value }}</span>
        </span>
        <span v-if="index != orderedAttributes.length - 1">, </span>
      </span>
    </div>

    <div v-if="expanded" class="child">
      <MapItemCollapsible
        v-for="child in item.children"
        :key="child"
        :item="child"
        :parent="item"
        :grandparent="parent"
        :highlight="highlight"
        :only-show-highlight="onlyShowHighlight"
        :out-of-bounds-only="outOfBoundsOnly"
      />
    </div>
  </div>
</template>

<script>
const isSearchResult = function (item, parent, highlight) {
  // decals and parallax stylegrounds match based on their texture path
  if (["decal", "parallax"].includes(item.name)) {
    if (
      item.attributes.texture !== undefined &&
      item.attributes.texture.toLowerCase().indexOf(highlight) >= 0
    ) {
      return true;
    }
  }

  // entities, triggers and effects match based on their node name
  if (
    parent !== null &&
    ["entities", "triggers", "Foregrounds", "Backgrounds"].includes(parent.name)
  ) {
    if (item.name.toLowerCase().indexOf(highlight) >= 0) {
      return true;
    }
  }

  return false;
};

const isOutOfBounds = function (item, parent, grandparent) {
  if (!["entities", "triggers"].includes(parent.name)) {
    return false;
  }

  if (
    typeof item.attributes.x !== "number" ||
    typeof item.attributes.y !== "number"
  ) {
    return false;
  }

  if (
    typeof grandparent.attributes.width !== "number" ||
    typeof grandparent.attributes.height !== "number"
  ) {
    return false;
  }

  const left = item.attributes.x;
  const top = item.attributes.y;
  const bottom =
    typeof item.attributes.height === "number"
      ? top + item.attributes.height
      : top;
  const right =
    typeof item.attributes.width === "number"
      ? left + item.attributes.width
      : left;

  return (
    right < 0 ||
    bottom < 0 ||
    top >= grandparent.attributes.height ||
    left >= grandparent.attributes.width
  );
};

const isHighlighted = function (
  item,
  parent,
  grandparent,
  highlight,
  outOfBoundsOnly,
) {
  // check if this should be highlighted due to being OOB only
  if (
    outOfBoundsOnly &&
    highlight === "" &&
    isOutOfBounds(item, parent, grandparent)
  ) {
    return true;
  }

  // check if this should be highlighted due to being a search result
  if (
    highlight !== "" &&
    (!outOfBoundsOnly || isOutOfBounds(item, parent, grandparent))
  ) {
    if (isSearchResult(item, parent, highlight)) {
      return true;
    }
  }

  // check if any child is highlighted, so we should be too
  for (const child of item.children) {
    if (isHighlighted(child, item, parent, highlight, outOfBoundsOnly)) {
      return true;
    }
  }

  // all checks failed!
  return false;
};

export default {
  props: [
    "item",
    "highlight",
    "parent",
    "grandparent",
    "onlyShowHighlight",
    "outOfBoundsOnly",
  ],
  data: () => ({
    expanded: false,
  }),
  computed: {
    orderedAttributes() {
      const specialKeys = ["name", "x", "y", "width", "height", "id"];
      const result = [];
      const specials = [];
      for (const [key, value] of Object.entries(this.item.attributes)) {
        if (specialKeys.includes(key)) {
          specials.push({ key, value });
        } else {
          result.push({ key, value });
        }
      }
      specials.sort(
        (a, b) => specialKeys.indexOf(a.key) - specialKeys.indexOf(b.key),
      );
      result.sort((a, b) =>
        a.key.toLowerCase().localeCompare(b.key.toLowerCase()),
      );

      specials.push(...result);
      return specials;
    },
    highlighted() {
      if (this.onlyShowHighlight) return false;
      return isHighlighted(
        this.item,
        this.parent,
        this.grandparent,
        this.highlight.toLowerCase(),
        this.outOfBoundsOnly,
      );
    },
    shown() {
      if (!this.onlyShowHighlight) return true;
      return isHighlighted(
        this.item,
        this.parent,
        this.grandparent,
        this.highlight.toLowerCase(),
        this.outOfBoundsOnly,
      );
    },
  },
  methods: {
    expandOrCollapse: function () {
      this.expanded = !this.expanded;
    },
  },
};
</script>

<style lang="scss" scoped>
.child {
  margin-left: 20px;
}

.clickable {
  cursor: pointer;
  user-select: none;
}

.tree-item {
  margin-top: 2px;
  margin-bottom: 2px;
  display: inline-block;
}

.highlight {
  padding: 3px;
  background-color: yellow;
  border: solid black 3px;
  border-radius: 10px;
}

@media (prefers-color-scheme: dark) {
  .highlight {
    border: solid lightgray 3px;
    background-color: #660;
  }
}

.arrow {
  display: inline-block;
  width: 1.25rem;
  height: 1.25rem;
  margin-right: 5px;

  &.dark {
    display: none;
  }

  @media (prefers-color-scheme: dark) {
    &.dark {
      display: inline-block;
    }
    &.light {
      display: none;
    }
  }

  &:not(.pointer) {
    text-align: center;
  }

  &.pointer {
    margin-bottom: 2px; // I don't get vertical alignment in CSS and gave up on trying
    transition: transform 0.2s ease-in-out;

    @media (prefers-reduced-motion: reduce) {
      transition: none;
    }

    &.collapsed {
      transform: rotate(-90deg);
    }
  }
}

.attributes {
  padding-left: 26px;
}

.attribute {
  color: gray;
  font-style: italic;
  font-size: 10pt;
  font-weight: bold;
}
.att-value {
  font-weight: normal;
}
</style>
