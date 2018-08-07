package edu.artic.map

import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticObject

sealed class MapItem<T>(val item: T, val floor: Int) {
    class Annotation(item: ArticMapAnnotation, floor: Int) : MapItem<ArticMapAnnotation>(item, floor)
    class Gallery(gallery: ArticGallery, floor: Int) : MapItem<ArticGallery>(gallery, floor)
    class Object(item: ArticObject, floor: Int) : MapItem<ArticObject>(item, floor)
}