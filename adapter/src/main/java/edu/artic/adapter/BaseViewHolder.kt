package edu.artic.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

/**
 * Description: ViewHolder that holds a required bindingSafe.
 */
open class BaseViewHolder(
    viewGroup: ViewGroup,
    @LayoutRes
    val layout: Int,
) :
    RecyclerView.ViewHolder(
        LayoutInflater.from(viewGroup.context)
            .inflate(layout, viewGroup, false)
    ) {

    private var _binding: ViewBinding? = null
    val binding: ViewBinding
        get() = _binding ?: throw IllegalStateException("Binding is not available yet.")

    init {
        Log.d("TAG", ": ")

        val vbClass =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<ViewBinding>
        val inflateMethod = vbClass.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        _binding = inflateMethod.invoke(
            null,
            LayoutInflater.from(viewGroup.context),
            viewGroup,
            false
        ) as ViewBinding
    }


    val context: Context
        get() = itemView.context
}
