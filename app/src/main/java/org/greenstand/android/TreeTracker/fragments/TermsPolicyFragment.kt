package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_terms_policy.*
import kotlinx.android.synthetic.main.fragment_terms_policy.view.*
import org.greenstand.android.TreeTracker.R


class TermsPolicyFragment: Fragment(){



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_terms_policy, container,false)
        requireActivity().toolbarTitle?.apply {
            setText(R.string.sign_up_title)
            setTextColor(resources.getColor(R.color.blackColor)) }
        val extras = arguments

        //Make parts of the text_agreement to be clickable
        val spannableString = SpannableString(getString(R.string.agreement_text_test))

        val clickableTermsCond = object : ClickableSpan(){
            override fun onClick(widget: View) {
                if(view.terms_text.visibility == View.VISIBLE){
                    view.terms_text.visibility = View.GONE
                }else view.terms_text.visibility = View.VISIBLE
            }
            override fun updateDrawState(drawState: TextPaint) {
                super.updateDrawState(drawState)
                drawState.isUnderlineText = true
                drawState.color = resources.getColor(R.color.redColor)
            }

        }
        val clickablePolicy = object : ClickableSpan(){
            override fun onClick(widget: View) {
                if(view.policy_text.visibility == View.VISIBLE){
                    view.policy_text.visibility = View.GONE
                }else view.policy_text.visibility = View.VISIBLE

            }
            override fun updateDrawState(drawState: TextPaint) {
                super.updateDrawState(drawState)
                drawState.isUnderlineText = true
                drawState.color = resources.getColor(R.color.redColor)
            }

        }

        val clickableCookies = object : ClickableSpan(){
            override fun onClick(widget: View) {
                if(view.cookies_text.visibility == View.VISIBLE){
                    view.cookies_text.visibility = View.GONE
                }else view.cookies_text.visibility = View.VISIBLE

            }
            override fun updateDrawState(drawState: TextPaint) {
                super.updateDrawState(drawState)
                drawState.isUnderlineText = true
                drawState.color = resources.getColor(R.color.redColor)
            }
        }

        spannableString.setSpan(clickableTermsCond, 32,48,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(clickablePolicy, 53,67,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(clickableCookies, 79,90,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        view.text_agreement.text = spannableString
        view.text_agreement.movementMethod = LinkMovementMethod.getInstance()

        view.show_details_text.setOnClickListener {
                terms_text?.visibility = View.VISIBLE
                policy_text?.visibility = View.VISIBLE
                cookies_text?.visibility = View.VISIBLE
            }


        view.accept_terms_button.setOnClickListener(object :View.OnClickListener{
            override fun onClick(v: View?) {
                val signupFragment = SignUpFragment()
                signupFragment.arguments = extras
                val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
                fragmentTransaction?.addToBackStack(null)?.replace(R.id.containerFragment, signupFragment)
                fragmentTransaction?.commit()

            }
        })

        return view

    }

}