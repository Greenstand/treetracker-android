package org.greenstand.android.TreeTracker.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_user_details.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [UserDetailsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [UserDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class UserDetailsFragment : androidx.fragment.app.Fragment() {

    private var mSharedPreferences: SharedPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_user_details, container, false)

        mSharedPreferences = activity!!.getSharedPreferences(
            ValueHelper.NAME_SPACE, Context.MODE_PRIVATE
        )

        val continueButton: Button = v.fragmentUserDetailsContinue
        continueButton.setOnClickListener {
            val firstNameTextView = v.fragmentUserDetailsFirstName
            val lastNameTextView = v.fragmentUserDetailsLastName
            val organizationTextView = v.fragmentUserDetailsOrganization
            val privacyPolicyCheckbox = v.fragmentSignupPrivacyPolicyCheckbox
            val planterIdentifier = mSharedPreferences?.getString(ValueHelper.PLANTER_IDENTIFIER, null)

            var dataReady = true // TODO: handle form errors and required fields
            if (firstNameTextView.text == null || firstNameTextView.text.isEmpty()) {
                dataReady = false
            } else if (lastNameTextView.text == null || firstNameTextView.text.isEmpty()) {
                dataReady = false
            } else if (!privacyPolicyCheckbox.isChecked) {
                dataReady = false
            }

            if (!dataReady || planterIdentifier == null) {
                // data inconsistency
                // TODO: handle this somehow
            } else {

                val planterDetailsEntity = PlanterDetailsEntity(
                    planterIdentifier,
                    firstNameTextView.text.toString(),
                    lastNameTextView.text.toString(),
                    organizationTextView.text.toString(),
                    null,
                    null,
                    false,
                    Utils.dateFormat.format(Date())
                )

                GlobalScope.launch {

                    val planterDetailsId =
                        TreeTrackerApplication.getAppDatabase().planterDao().insert(planterDetailsEntity)

                    val planterIdentifications = TreeTrackerApplication.getAppDatabase().planterDao()
                        .getPlanterIdentificationsByID(planterIdentifier).first()
                    planterIdentifications.planterDetailsId = planterDetailsId
                    TreeTrackerApplication.getAppDatabase().planterDao()
                        .updatePlanterIdentification(planterIdentifications)
                }

                val editor = mSharedPreferences?.edit()
                val tsLong = System.currentTimeMillis() / 1000
                editor?.putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, tsLong)
                editor?.apply()

                activity!!.supportFragmentManager.popBackStack()

                val fragmentTransaction = activity!!.supportFragmentManager
                    .beginTransaction()
                val fragment = MapsFragment()
                fragmentTransaction.replace(R.id.containerFragment, fragment).commit()

            }
        }
        val fragment_signup_privacy_policy_link = v.fragmentSignupPrivacyPolicyLink
        fragment_signup_privacy_policy_link.movementMethod = LinkMovementMethod.getInstance()

        return v
    }
}
