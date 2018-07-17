package edu.artic.welcome

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.base.emptyString
import edu.artic.db.models.ArticExhibition
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.exhibition_layout.view.*

/**
 * @author Sameer Dhakal (Fuzz)
 */

class OnViewAdapter : AutoHolderRecyclerViewAdapter<OnViewViewModel>() {
    override fun View.onBindView(item: OnViewViewModel, position: Int) {
        item.exhibitionTitleStream
                .bindToMain(exhibitionTitle.text())
                .disposedBy(item.viewDisposeBag)

        item.exhibitionDate
                .bindToMain(exhibitionDate.text())
                .disposedBy(item.viewDisposeBag)

        item.exhibitionImageUrl
                .filter { it.isNotEmpty() }
                .subscribe {
                    Glide.with(context)
                            .load(it)
                            .into(image)
                }.disposedBy(item.viewDisposeBag)
    }

    override fun getLayoutResId(position: Int): Int {
        return R.layout.exhibition_layout
    }

}


class OnViewViewModel(exhibition: ArticExhibition) : BaseViewModel() {
    val exhibitionTitleStream: Subject<String> = BehaviorSubject.createDefault(exhibition.title)
    val exhibitionDate: Subject<String> = BehaviorSubject.createDefault("2017 August 9")
    val exhibitionImageUrl: Subject<String> = BehaviorSubject.createDefault(exhibition.image_url
            ?: "")
}
