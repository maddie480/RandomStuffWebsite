<template>
  <div class="collapsible">
    <div class="line">
      <span>
        <span v-if="folder.children.length === 0" class="arrow">–</span>
        <span v-else class="clickable" @click="expandOrCollapse">
          <img
            :class="'arrow pointer dark' + (expanded ? '' : ' collapsed')"
            src="/img/arrow-white.svg"
          />
          <img
            :class="'arrow pointer light' + (expanded ? '' : ' collapsed')"
            src="/img/arrow-black.svg"
          />
        </span>

        <a
          v-if="folder.files.length !== 0"
          :class="
            'item-name' +
            (selectedFolder.path === folder.path ? ' selected' : '')
          "
          href="#"
          @click="$emit('select-folder', folder)"
          >{{ folder.name }}</a
        >
        <span v-else class="item-name">{{ folder.name }}</span>

        <span class="smaller">({{ fileCount }})</span>
      </span>
    </div>

    <div v-if="expanded" class="child">
      <GraphicsDumpFolder
        v-for="child in folder.children"
        :key="child"
        :folder="child"
        :selected-folder="selectedFolder"
        @select-folder="(folder) => $emit('select-folder', folder)"
      />
    </div>
  </div>
</template>

<script>
const countFilesRecursive = function (folder) {
  let count = folder.files.length;
  for (let i = 0; i < folder.children.length; i++) {
    count += countFilesRecursive(folder.children[i]);
  }
  return count;
};

export default {
  props: {
    folder: { type: Array, required: true },
    selectedFolder: { type: Object, required: true },
  },
  emits: ["select-folder"],
  data: () => ({
    expanded: false,
  }),
  computed: {
    fileCount: function () {
      return countFilesRecursive(this.folder);
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

.selected {
  font-weight: bold;
}

.smaller {
  font-size: 10pt;
  font-style: italic;
  margin-left: 5px;
  color: #666;

  @media (prefers-color-scheme: dark) {
    color: #888;
  }
}
</style>
