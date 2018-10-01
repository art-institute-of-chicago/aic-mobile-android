#! /bin/bash

generateZoomLevels(){
  FILENAME="$1"
  ZOOM_LEVEL="$2"
  FLOOR="$3"

  # 512px is the size of a singular tile
  # generating the size of the image we need to the power of 2 of the and current zoom level
  OUTPUT_SIZE=$((512*(2**$ZOOM_LEVEL)))

  # generate the full SIZE image background
  magick convert -size "${OUTPUT_SIZE}x${OUTPUT_SIZE}" xc:"rgb(82,122,157)" tileBg.jpg

  # calculated width of the image from the pdf to be used to cover the museum bounds within the Tile matrix
  # this value is scaled from the OUTPUT_SIZE (currently 0.905)
  SCALED_WIDTH=$(bc <<< "scale=0; (${OUTPUT_SIZE}*0.905)/1")

  # calculated height of the image from the pdf to be used to cover the museum bounds within the Tile matrix
  # this value is scaled from the OUTPUT_SIZE (currently 0.8974)
  SCALED_HEIGHT=$(bc <<< "scale=0; (${OUTPUT_SIZE}*0.8974)/1")

  #generate a scaled down and rotated by -1 degree image from PDF
  magick convert -density 300 $FILENAME -scale "${SCALED_WIDTH}x${SCALED_HEIGHT}!" -background "rgb(82,122,157)" -rotate "-1" scaledAndRotated.jpg

  # chop off a portion of the image to push the image down into it's correct position on Google Maps
  # The left side is to the west on Google Maps and will be flush with the full scale image at OUTPUT_SIZE
  LEFT_CHOP_AMOUNT_SCALED=$(((31*(2**${ZOOM_LEVEL}))))
  magick convert scaledAndRotated.jpg -chop "${LEFT_CHOP_AMOUNT_SCALED}x0" chopped.jpg

  # composite the previously chopped image on to the generated tileBg image
  # the chopped image is offset vertically by this offset to adjust for the north/south offset
  VERTICAL_OFFSET_MODIFIER=$((34*(2**$ZOOM_LEVEL)))
  magick composite -gravity northWest -geometry "+0+${VERTICAL_OFFSET_MODIFIER}" chopped.jpg tileBg.jpg tile.jpg

  # create directory if not already created
  mkdir -p "floor${FLOOR}/zoom${ZOOM_LEVEL}"

  # split previously composited image into a bunch of 512x512 tiles
  magick convert tile.jpg -crop 512x512 -quality 82 -filter Triangle -define filter:support=2 -define jpeg:fancy-upsampling=off -colorspace sRGB -strip "floor${FLOOR}/zoom${ZOOM_LEVEL}/tiles.jpg"
  echo "zoome level: $ZOOM_LEVEL tiles generated for floor $FLOOR"
}

# generate the various zoom levels used by the app
# pass in the pdf file, the request zoom level and floor number
# zoom level 1 does not seem to be used at this point
# generateZoomLevels "$1" "1" "$2"
generateZoomLevels "$1" "2" "$2"
generateZoomLevels "$1" "3" "$2"
generateZoomLevels "$1" "4" "$2"
generateZoomLevels "$1" "5" "$2"
# we only support these four zoom levels as we max it out, but you could add more below
# additionally each new zoom level takes twice as long to run 5 currently takes about 10 to 15 minutes
# generateZoomLevels "$1" "6" "$2"
