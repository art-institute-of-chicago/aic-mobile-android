#! /bin/bash

# generate the various zoom levels used by the app
#
# pass in the pdf file and floor number
# usage: ./tiler.sh path/to/map.pdf 2

FLOORS_DIR='map/floors'
STAGING_DIR='/tmp/map'
BACKGROUND_FILE="${STAGING_DIR}/tileBg.jpg"
CROP_FILE="${STAGING_DIR}/chopped.jpg"
TILE_FILE="${STAGING_DIR}/tile.jpg"
TRANSFORM_FILE="${STAGING_DIR}/scaledAndRotated.jpg"

BACKGROUND_COLOR="rgb(82,122,157)"
TILE_SIZE=512 # in pixels

# check that the required dependencies with the correct versions are installed
checkDependencies(){
  if (! hash magick 2>/dev/null) || (! magick -version | grep "Version: ImageMagick 7." &>/dev/null); then
    echo "You must install ImageMagick version 7: https://imagemagick.org/"
    exit 1
  fi
  if ! hash bc 2>/dev/null; then
    echo "You must install bc: https://www.gnu.org/software/bc/"
    exit 1
  fi
}

# create a /tmp directory for transient files
setup(){
  mkdir -p $STAGING_DIR
}

# remove transient files
cleanup(){
  rm -rf $STAGING_DIR
}

generateZoomLevels(){
  local FILENAME="$1"
  local ZOOM_LEVEL="$2"
  local FLOOR="$3"

  printf "generating tiles for zoom level ${ZOOM_LEVEL}..."

  # 512px is the size of a singular tile
  # generating the size of the image we need to the power of 2 of the and current zoom level
  OUTPUT_SIZE=$(($TILE_SIZE*(2**$ZOOM_LEVEL)))

  # generate the full SIZE image background
  magick -size "${OUTPUT_SIZE}x${OUTPUT_SIZE}" xc:"${BACKGROUND_COLOR}" $BACKGROUND_FILE

  # calculated width of the image from the pdf to be used to cover the museum bounds within the Tile matrix
  # this value is scaled from the OUTPUT_SIZE (currently 0.907)
  # increasing or decreasing this value by 0.001 will add/subtract 2 pixels from IMG_SIZE
  IMG_SIZE=$(bc <<< "scale=0; (${OUTPUT_SIZE}*0.907)/1")

  #generate a scaled down and rotated by -1 degree image from PDF
  # This accounts for the slight angle that the museum is at in relation to the google map tiles
  magick \
    -density 300 $FILENAME \
    -scale "${IMG_SIZE}x${IMG_SIZE}" \
    -background "${BACKGROUND_COLOR}" \
    -rotate "-1.00" $TRANSFORM_FILE

  # used to calculate half pixels in calculations belows
  HALF_PIXEL=$(((1*(2**(${ZOOM_LEVEL}-1)))))

  # chop off a portion of the image to push the image down into it's correct position on Google Maps
  # The left side is to the west on Google Maps and will be flush with the full scale image at OUTPUT_SIZE
  # if you need to add or subtract a half pixel do so
  LEFT_CHOP_AMOUNT_SCALED=$(((31*(2**${ZOOM_LEVEL}))))
  LEFT_CHOP_AMOUNT_SCALED=$((${LEFT_CHOP_AMOUNT_SCALED}+${HALF_PIXEL}))
  magick $TRANSFORM_FILE -chop "${LEFT_CHOP_AMOUNT_SCALED}x0" $CROP_FILE

  # composite the previously chopped image on to  the generated tileBg image
  # the chopped image is offset vertically by this offset to adjust for the north/south offset
  # if you need to add or subtract a half pixel do so
  VERTICAL_OFFSET_MODIFIER=$((31*(2**$ZOOM_LEVEL)))
  #VERTICAL_OFFSET_MODIFIER=$((${VERTICAL_OFFSET_MODIFIER}-${HALF_PIXEL}))
  magick composite \
    -gravity northWest \
    -geometry "+0+${VERTICAL_OFFSET_MODIFIER}" $CROP_FILE $BACKGROUND_FILE $TILE_FILE

  # create directory if not already created
  TILES_DIR="${FLOORS_DIR}/floor${FLOOR}/zoom${ZOOM_LEVEL}"
  mkdir -p $TILES_DIR

  # split previously composited image into a bunch of 512x512 tiles
  magick $TILE_FILE \
    -crop "${TILE_SIZE}x${TILE_SIZE}" \
    -quality 82 \
    -filter Triangle \
    -define filter:support=2 \
    -define jpeg:fancy-upsampling=off \
    -colorspace sRGB \
    -strip "${TILES_DIR}/tiles.jpg"

  # overwrite the previous status line and display the tile count
  TILE_COUNT=$(ls -1 $TILES_DIR | wc -l | awk '{print $1}')
  printf  "\r\033[2Kfloor: $FLOOR | zoom level: $ZOOM_LEVEL | tiles: $TILE_COUNT\n"
}

checkDependencies
setup

# we only support these five zoom levels (2 through 6) as we max it out, but you could add more
# zoom level 1 does not seem to be used at this point
FILENAME="$1"
if [ -z $FILENAME ]; then
  echo "You must provide a map file"
  exit 1
fi
FLOOR="$2"
if [ -z $FLOOR ]; then
  echo "You must specify a floor"
  exit 1
fi
for ZOOM_LEVEL in {2..6}; do
  generateZoomLevels "$FILENAME" "$ZOOM_LEVEL" "$FLOOR"
done

cleanup
