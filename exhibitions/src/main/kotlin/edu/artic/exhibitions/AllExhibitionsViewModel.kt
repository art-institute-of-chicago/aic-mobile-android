package edu.artic.exhibitions

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.daos.ArticExhibitionDao
import edu.artic.db.models.ArticExhibition
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class AllExhibitionsViewModel @Inject constructor(exhibitionsDao: ArticExhibitionDao) : BaseViewModel() {

    val exhibitions: Subject<List<AllExhibitionsCellViewModel>> = BehaviorSubject.create()

    init {
        exhibitionsDao.getAllExhibitions()
                .map { list ->
                    val viewModelList = ArrayList<AllExhibitionsCellViewModel>()
                    list.forEach { exhibition ->
                        viewModelList.add(AllExhibitionsCellViewModel(exhibition))
                    }
                    return@map viewModelList
                }
                .bindTo(exhibitions)
                .disposedBy(disposeBag)
    }

}

class AllExhibitionsCellViewModel(exhibition: ArticExhibition) : BaseViewModel() {
    val exhibitionTitle: Subject<String> = BehaviorSubject.createDefault(exhibition.title)
    val exhibitionDescription: Subject<String> = BehaviorSubject.createDefault(
            "Through ${exhibition.aic_end_at.format(
                    DateTimeHelper.HOME_EXHIBITION_DATE_FORMATTER
            )
            }"
    )
    val exhibitionImageUrl: Subject<String> = BehaviorSubject.createDefault(exhibition.legacy_image_mobile_url
            ?: "")
}