import jimp from "jimp/es";
import SWorker from "simple-web-worker";

/**
 * Converts a wipe image to an array of triangles
 * for usage in the game instead of loading full HD sprites.
 *
 * Each triangle is an array of length 3, each point being represented by an [x, y] array.
 *
 * @param {Buffer} input Image to convert to triangles
 * @returns {Array{Number}} An array of triangles, flattened and ready to be written to the bin file
 */
const convertWipeToTriangles = async (input) => {
  let image = await jimp.read(input);

  // try scanning vertically, then horizontally.
  const solution1 = await findTrianglesInWorker(image.clone(), true);
  const solution2 = await findTrianglesInWorker(image, false);

  // we want to retain the solution with the least amount of triangles.
  return solution1.length < solution2.length ? solution1 : solution2;
};

/**
 * Seeks for triangles in a worker thread.
 * @param {Object} bitmap Jimp image to convert to triangles
 * @param {Boolean} verticalScan true to go through the image vertically first, false to go through it horizontally first
 * @returns {Array{Number}} An array of triangles, flattened and ready to be written to the bin file
 */
const findTrianglesInWorker = (image, verticalScan) => {
  // Web workers are in their own space, you can only give them primitive values and get back primitive values,
  // and you cannot call functions from outside of the web worker too... so that looks a bit weird.
  // Since you cannot even use Jimp functions, two of them are reimplemented here.
  return SWorker.run(
    (bitmap, verticalScan) => {
      /**
       * Returns the offset of a pixel in the bitmap buffer.
       * Overly simplified implementation of Jimp.getPixelIndex(x, y).
       * @param {Object} bitmap The bitmap to consider
       * @param {number} x the x coordinate
       * @param {number} y the y coordinate
       * @returns {number} the index of the pixel
       */
      const getPixelIndex = (bitmap, x, y) => {
        return (bitmap.width * y + x) << 2;
      };

      /**
       * Runs a function on every pixel of the zone.
       * Taken from @jimp/utils.
       * @param {Object} bitmap The bitmap to scan
       * @param {number} x The left of the area to scan
       * @param {number} y The top of the area to scan
       * @param {number} w The width of the scan
       * @param {number} h The height of the scan
       * @param {Function} f The function to run on every pixel of the zone
       */
      const scan = (bitmap, x, y, w, h, f) => {
        // round input
        x = Math.round(x);
        y = Math.round(y);
        w = Math.round(w);
        h = Math.round(h);

        for (let _y = y; _y < y + h; _y++) {
          for (let _x = x; _x < x + w; _x++) {
            const idx = (bitmap.width * _y + _x) << 2;
            f.call(bitmap, _x, _y, idx);
          }
        }
      };

      /**
       * Seeks for triangles going with this strategy:
       * - go through the image looking for a black pixel
       * - extend a rectangle from this pixel
       * - paint the rectangle white and save it in the form of 2 triangles
       * - repeat
       * @param {Object} bitmap Jimp image to convert to triangles
       * @param {Boolean} verticalScan true to go through the image vertically first, false to go through it horizontally first
       * @returns {Array{Array{Array{Number}}}} An array of triangles
       */
      const findTriangles = (bitmap, verticalScan) => {
        const result = [];

        // scan the image looking for a black pixel.
        let x = 0,
          y = 0;
        while (x < bitmap.width && y < bitmap.height) {
          const idx = getPixelIndex(bitmap, x, y);
          if (isBlack(bitmap.data.slice(idx, idx + 4))) {
            // black pixel! extend rectangle from here (vertically first, then horizontally first).
            const dimensions1 = extendRectangleFrom(bitmap, x, y, true);
            const dimensions2 = extendRectangleFrom(bitmap, x, y, false);

            // we want to keep the rectangle with the biggest area, and remove it from the image.
            let definitiveTriangles;
            if (
              dimensions1[0] * dimensions1[1] >
              dimensions2[0] * dimensions2[1]
            ) {
              definitiveTriangles = getTrianglesFromRectangle(
                x,
                y,
                dimensions1[0],
                dimensions1[1]
              );
              scan(
                bitmap,
                x,
                y,
                dimensions1[0],
                dimensions1[1],
                (_x, _y, idx) => (bitmap.data[idx + 3] = 0)
              );
            } else {
              definitiveTriangles = getTrianglesFromRectangle(
                x,
                y,
                dimensions2[0],
                dimensions2[1]
              );
              scan(
                bitmap,
                x,
                y,
                dimensions2[0],
                dimensions2[1],
                (_x, _y, idx) => (bitmap.data[idx + 3] = 0)
              );
            }

            // and save them to the list.
            definitiveTriangles.forEach((s) => result.push(s));
          }

          if (verticalScan) {
            // move down, then right if we reached the bottom
            y++;
            if (y >= bitmap.height) {
              y = 0;
              x++;
            }
          } else {
            // move right, then down if we reached the bottom
            x++;
            if (x >= bitmap.width) {
              x = 0;
              y++;
            }
          }
        }

        return result;
      };

      /**
       * Extends a rectangle as far as possible from the given top-left position, until it hits a non-black pixel.
       * @param {Jimp} bitmap Jimp image to convert to triangles
       * @param {Number} x the starting X position
       * @param {Number} y the starting Y position
       * @param {Boolean} verticalScan true to extend the rectangle vertically first, false to extend it horizontally first
       * @returns {Array{Number}} the width and height of the rectangle found
       */
      const extendRectangleFrom = (bitmap, x, y, verticalScan) => {
        let width = 1,
          height = 1;
        if (verticalScan) {
          // extend height, then width, while the contents of the rectangle are all black.
          let idx = getPixelIndex(bitmap, x, y + height);
          while (
            y + height < bitmap.height &&
            isBlack(bitmap.data.slice(idx, idx + 4))
          ) {
            height++;
            idx = getPixelIndex(bitmap, x, y + height);
          }
          while (
            x + width < bitmap.width &&
            rectangleIsAllBlack(bitmap, x + width, y, 1, height)
          ) {
            width++;
          }
        } else {
          // extend width, then height, while the contents of the rectangle are all black.
          let idx = getPixelIndex(bitmap, x + width, y);
          while (
            x + width < bitmap.width &&
            isBlack(bitmap.data.slice(idx, idx + 4))
          ) {
            width++;
            idx = getPixelIndex(bitmap, x + width, y);
          }
          while (
            y + height < bitmap.height &&
            rectangleIsAllBlack(bitmap, x, y + height, width, 1)
          ) {
            height++;
          }
        }
        return [width, height];
      };

      /**
       * Checks if all the pixels in the given rectangle are black.
       * @param {Object} bitmap Jimp image to convert to triangles
       * @param {Number} x the starting X position
       * @param {Number} y the starting Y position
       * @param {*} width the width of the rectangle
       * @param {*} height the height of the rectangle
       * @returns {Boolean} whether all the pixels in the rectangle are black.
       */
      const rectangleIsAllBlack = (bitmap, x, y, width, height) => {
        let allBlack = true;
        scan(bitmap, x, y, width, height, (_x, _y, idx) => {
          if (!isBlack(bitmap.data.slice(idx, idx + 4))) {
            allBlack = false;
          }
        });

        return allBlack;
      };

      /**
       * Checks if the color given is black (or at least somewhat black).
       * @param {Array{Number}} color an array with the 4 components of the color: [r, g, b, a]
       * @returns whether the given color is black
       */
      const isBlack = (color) => {
        return (
          color[0] < 128 && color[1] < 128 && color[2] < 128 && color[3] > 128
        );
      };

      /**
       * Splits a rectangle into 2 triangles.
       *
       * @param {Number} x the starting X position
       * @param {Number} y the starting Y position
       * @param {*} width the width of the rectangle
       * @param {*} height the height of the rectangle
       * @returns {Array{Array{Number}}} an array containing 2 triangles
       */
      const getTrianglesFromRectangle = (x, y, width, height) => {
        const tri1 = [
          [x, y],
          [x + width, y],
          [x + width, y + height],
        ];
        const tri2 = [
          [x, y],
          [x, y + height],
          [x + width, y + height],
        ];
        return [tri1, tri2];
      };

      // Actually call the function!
      const triangles = findTriangles(bitmap, verticalScan);

      // double-flatten (triangles => points => coordinates) to end up with a coordinate list
      return triangles.flatMap((t) => t).flatMap((p) => p);
    },
    [image.bitmap, verticalScan]
  );
};

export default { convertWipeToTriangles };
