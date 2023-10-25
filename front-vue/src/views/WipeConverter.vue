<template>
  <div class="wipe-converter">
    <h1>Wipe Converter</h1>

    <div class="text col-lg-8 offset-lg-2">
      <p>
        Use this tool to convert your wipes from a series of PNG files to the
        format expected by
        <a
          href="https://gamebanana.com/mods/53687"
          rel="noopener"
          target="_blank"
          >Maddie's Helping Hand</a
        >, to use them in-game.
        <b>It is recommended to have 60 frames in total</b> (30 frames for the
        "fade in" animation, 30 for "fade out").
      </p>
      <p>
        Select all your "fade in" or "fade out" frames, hit "Convert", and be
        patient!
      </p>
      <div class="alert alert-info">
        The frames will be converted in alphabetical order. If you numbered them
        and have 10 frames or more,
        <b
          >make sure the first frames are numbered <code>00</code> to
          <code>09</code></b
        >
        (not <code>0</code> to <code>9</code>)!
      </div>
      <div>
        Once you got the file:
        <ul>
          <li>
            rename it to:
            <ul>
              <li>
                <code>death-wipe.bin</code> (or <code>wipe-out.bin</code>) for
                the fade to black after death
              </li>
              <li>
                <code>spawn-wipe.bin</code> (or <code>wipe-in.bin</code>) for
                the fade in when you respawn
              </li>
            </ul>
          </li>
          <li>
            move it to your mod, in
            <code>Mods/yourmod/MaxHelpingHandWipes/yournickname/wipename</code>
            folder
          </li>
          <li>
            in your map editor, use
            <code>MaxHelpingHand/CustomWipe:yournickname/wipename</code> as a
            wipe
          </li>
        </ul>
      </div>
    </div>

    <label class="btn btn-default">
      <input
        type="file"
        accept="image/png"
        multiple
        @change="selectFile"
        :disabled="converting"
      />
    </label>

    <button
      class="btn btn-primary"
      :disabled="!selectedFiles || converting"
      @click="convert"
    >
      Convert!
    </button>

    <div class="error" v-if="error">
      <div class="warning">
        An error occurred. Check that your images are valid.
      </div>
    </div>

    <div class="converting" v-if="converting">Converting...</div>
    <div class="progress" v-if="converting">
      <div
        class="progress-bar progress-bar-info"
        role="progressbar"
        :aria-valuenow="fileProgress"
        aria-valuemin="0"
        :aria-valuemax="selectedFiles.length"
        :style="{ width: (fileProgress / selectedFiles.length) * 100 + '%' }"
      >
        {{ fileProgress }} / {{ selectedFiles.length }}
      </div>
    </div>
  </div>
</template>

<script>
import download from "downloadjs";
import wipeConverter from "../services/wipe-converter";

const vue = {
  name: "wipe-converter",
  data: () => ({
    selectedFiles: undefined,
    converting: false,
    error: false,
    fileProgress: 0,
  }),
  methods: {
    selectFile: function () {
      this.progressInfos = [];
      this.selectedFiles = event.target.files;
    },
    convert: async function () {
      try {
        this.converting = true;
        this.error = false;

        // sort files by name
        const selectedFiles = Array.from(this.selectedFiles).sort((a, b) =>
          a.name.localeCompare(b.name),
        );

        // the output bin is a binary format:
        // [frame count], for each frame { [coordinate count], [coordinate list] }
        // all numbers are encoded on 2 bytes, except [coordinate count] because it may exceed 65535.

        // first, we are going to write the frame count
        const result = [];
        result.push(selectedFiles.length);

        // then, convert them 1 by 1
        for (let i = 0; i < selectedFiles.length; i++) {
          this.fileProgress = i;

          // read the image as an ArrayBuffer
          const fileBuffer = await new Promise((resolve, reject) => {
            const reader = new FileReader();

            reader.onload = (e) => {
              resolve(e.target.result);
            };

            reader.onerror = (error) => {
              reject(error);
            };

            reader.readAsArrayBuffer(selectedFiles[i]);
          });

          // convert it to triangles
          const coordinates = await wipeConverter.convertWipeToTriangles(
            Buffer.from(fileBuffer),
          );

          // add the coordinate count, then the coordinates themselves, to the array.
          result.push(coordinates.length % 65536);
          result.push(Math.floor(coordinates.length / 65536));
          coordinates.forEach((c) => result.push(c));
        }

        // download the result as a bin file!
        download(
          new Blob([new Uint16Array(result)], {
            type: "application/octet-stream",
          }),
          "wipe.bin",
          "application/octet-stream",
        );
      } catch (e) {
        this.error = true;
      }
      this.converting = false;
    },
  },
};

export default vue;
</script>

<style lang="scss" scoped>
h1 {
  margin-bottom: 30px;
}

.converting {
  font-size: 18pt;
  margin: 20px;
}

.error {
  font-size: 18pt;
  color: #ff8000;
  margin: 20px;
}

.text {
  text-align: left;
  margin-bottom: 30px;
}
</style>
