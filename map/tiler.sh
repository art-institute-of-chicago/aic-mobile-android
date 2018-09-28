#! /bin/bash

generateZoomLevels(){
  FILENAME="$1"
  ZOOM_LEVEL="$2"
  FLOOR="$3"

  SIZE=$((512*(2**$ZOOM_LEVEL)))

  # should the scale value (currently 0.905) be played with modifcations need to
  # also occur in $LEFT_CHOP_AMOUNT_SCALED
  SCALED_WIDTH=$(bc <<< "scale=0; (${SIZE}*0.905)/1")

  # should the scale value (currently 0.8974) be played with modifcations need to
  # also occur in $VERTICAL_OFFSET_MODIFIER
  SCALED_HEIGHT=$(bc <<< "scale=0; (${SIZE}*0.8974)/1")

  PAD_AMOUNT_SCALED=$((1*(2**${ZOOM_LEVEL})))
  echo $PAD_AMOUNT_SCALED

  # generated value for use in removing that amount from the scaledAndRotated.jpg image
  LEFT_CHOP_AMOUNT_SCALED=$(((30*(2**${ZOOM_LEVEL}))+${PAD_AMOUNT_SCALED}))

  #generate a scaled scaled down and rotated by -1 degree image from PDF
  #echo "magick convert -density 300 $FILENAME -scale \"${SCALED_WIDTH}x${SCALED_HEIGHT}\" -rotate \"-1.5\" -background transparent scaledAndRotated.jpg"
  magick convert -density 300 $FILENAME -scale "${SCALED_WIDTH}x${SCALED_HEIGHT}!" -background "rgb(82,122,157)" -rotate "-1" scaledAndRotated.jpg 

  #removes a dynamically checked cut
  magick convert scaledAndRotated.jpg -background "rgb(82,122,157)" -splice "0x${PAD_AMOUNT_SCALED}" -gravity east -background "rgb(82,122,157)" -splice "${PAD_AMOUNT_SCALED}x0" padded.jpg
  magick convert padded.jpg -chop "${LEFT_CHOP_AMOUNT_SCALED}x0" -gravity south -chop "0x${PAD_AMOUNT_SCALED}" chopped.jpg
  magick convert -size "${SIZE}x${SIZE}" xc:"rgb(82,122,157)" tileBg.jpg

  VERTICAL_OFFSET_MODIFIER=$((33*(2**$ZOOM_LEVEL)))

  # sets the scaled and cropped image into as close a place as possible to
  # where it should be on google's map
  magick composite -gravity northWest -geometry "+0+${VERTICAL_OFFSET_MODIFIER}" chopped.jpg tileBg.jpg tile.jpg

  # create directory if not already created
  mkdir -p "floor${FLOOR}/zoom${ZOOM_LEVEL}"

  # split previously composited image into a bunch of 512x512 tiles
  magick convert tile.jpg -crop 512x512 -quality 82 -filter Triangle -define filter:support=2 -define jpeg:fancy-upsampling=off -colorspace sRGB -strip "floor${FLOOR}/zoom${ZOOM_LEVEL}/tiles.jpg"
  echo "zoome level: $ZOOM_LEVEL tiles generated for floor $FLOOR"
}

# zoom level 1 does not seem to be used at this point
# generateZoomLevels "$1" "1" "$2"
generateZoomLevels "$1" "2" "$2"
generateZoomLevels "$1" "3" "$2"
generateZoomLevels "$1" "4" "$2"
generateZoomLevels "$1" "5" "$2"
# generateZoomLevels "$1" "6" "$2"
