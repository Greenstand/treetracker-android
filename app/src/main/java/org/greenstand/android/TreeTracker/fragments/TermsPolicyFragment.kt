package org.greenstand.android.TreeTracker.fragments

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_terms_policy.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.utilities.CameraHelper
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.viewmodels.TermsPolicyViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class TermsPolicyFragment: Fragment() {

    private val vm: TermsPolicyViewModel by viewModel()

    private val args: TermsPolicyFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        vm.userInfo = args.userInfo

        return inflater.inflate(R.layout.fragment_terms_policy, container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        requireActivity().toolbarTitle?.apply {
            setText(R.string.sign_up_title)
            setTextColor(resources.getColor(R.color.black))
        }

        //Make parts of the text_agreement to be clickable
//        val spannableString = SpannableString(getString(R.string.agreement_text_test))
//
//        val clickableTermsCond = object : ClickableSpan(){
//            override fun onClick(widget: View) {
//                if(view.terms_text.visibility == View.VISIBLE){
//                    view.terms_text.visibility = View.GONE
//                }else view.terms_text.visibility = View.VISIBLE
//            }
//            override fun updateDrawState(drawState: TextPaint) {
//                super.updateDrawState(drawState)
//                drawState.isUnderlineText = true
//                drawState.color = resources.getColor(R.color.redColor)
//            }
//
//        }
//        val clickablePolicy = object : ClickableSpan(){
//            override fun onClick(widget: View) {
//                if(view.policy_text.visibility == View.VISIBLE){
//                    view.policy_text.visibility = View.GONE
//                }else view.policy_text.visibility = View.VISIBLE
//
//            }
//            override fun updateDrawState(drawState: TextPaint) {
//                super.updateDrawState(drawState)
//                drawState.isUnderlineText = true
//                drawState.color = resources.getColor(R.color.redColor)
//            }
//
//        }
//
//        val clickableCookies = object : ClickableSpan(){
//            override fun onClick(widget: View) {
//                if(view.cookies_text.visibility == View.VISIBLE){
//                    view.cookies_text.visibility = View.GONE
//                }else view.cookies_text.visibility = View.VISIBLE
//
//            }
//            override fun updateDrawState(drawState: TextPaint) {
//                super.updateDrawState(drawState)
//                drawState.isUnderlineText = true
//                drawState.color = resources.getColor(R.color.redColor)
//            }
//        }

//        spannableString.setSpan(clickableTermsCond, 32,48,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        spannableString.setSpan(clickablePolicy, 53,67,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        spannableString.setSpan(clickableCookies, 79,90,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        text_agreement.text = spannableString
//        text_agreement.movementMethod = LinkMovementMethod.getInstance()
//
//        show_details_text.setOnClickListener {
//            terms_text?.visibility = View.VISIBLE
//            policy_text?.visibility = View.VISIBLE
//            cookies_text?.visibility = View.VISIBLE
//        }

        vm.onNavigateToMap = {
            findNavController().navigate(TermsPolicyFragmentDirections.actionTermsPolicyFragmentToMapsFragment())
        }

        accept_terms_button.setOnClickListener {
            CameraHelper.takePictureForResult(this, selfie = true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Permissions.MY_PERMISSION_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            CameraHelper.takePictureForResult(this, selfie = true)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode != Activity.RESULT_CANCELED) {
            if (resultCode == Activity.RESULT_OK) {
                vm.photoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH)
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("Photo was cancelled")
        }
    }

}