/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v4.app;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.IdRes;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.v4.os.BuildCompat;
import android.support.v4.util.ArraySet;
import android.support.v4.util.DebugUtils;
import android.support.v4.util.LogWriter;
import android.support.v4.util.Pair;
import android.support.v4.view.LayoutInflaterFactory;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Static library support version of the framework's {@link android.app.FragmentManager}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework {@link FragmentManager}
 * documentation for a class overview.
 *
 * <p>Your activity must derive from {@link FragmentActivity} to use this. From such an activity,
 * you can acquire the {@link FragmentManager} by calling
 * {@link FragmentActivity#getSupportFragmentManager}.
 */
public abstract class FragmentManager {
    /**
     * Representation of an entry on the fragment back stack, as created
     * with {@link FragmentTransaction#addToBackStack(String)
     * FragmentTransaction.addToBackStack()}.  Entries can later be
     * retrieved with {@link FragmentManager#getBackStackEntryAt(int)
     * FragmentManager.getBackStackEntryAt()}.
     *
     * <p>Note that you should never hold on to a BackStackEntry object;
     * the identifier as returned by {@link #getId} is the only thing that
     * will be persisted across activity instances.
     */
    public interface BackStackEntry {
        /**
         * Return the unique identifier for the entry.  This is the only
         * representation of the entry that will persist across activity
         * instances.
         */
        public int getId();

        /**
         * Get the name that was supplied to
         * {@link FragmentTransaction#addToBackStack(String)
         * FragmentTransaction.addToBackStack(String)} when creating this entry.
         */
        public String getName();

        /**
         * Return the full bread crumb title resource identifier for the entry,
         * or 0 if it does not have one.
         */
        @StringRes
        public int getBreadCrumbTitleRes();

        /**
         * Return the short bread crumb title resource identifier for the entry,
         * or 0 if it does not have one.
         */
        @StringRes
        public int getBreadCrumbShortTitleRes();

        /**
         * Return the full bread crumb title for the entry, or null if it
         * does not have one.
         */
        public CharSequence getBreadCrumbTitle();

        /**
         * Return the short bread crumb title for the entry, or null if it
         * does not have one.
         */
        public CharSequence getBreadCrumbShortTitle();
    }

    /**
     * Interface to watch for changes to the back stack.
     */
    public interface OnBackStackChangedListener {
        /**
         * Called whenever the contents of the back stack change.
         */
        public void onBackStackChanged();
    }

    /**
     * Start a series of edit operations on the Fragments associated with
     * this FragmentManager.
     *
     * <p>Note: A fragment transaction can only be created/committed prior
     * to an activity saving its state.  If you try to commit a transaction
     * after {@link FragmentActivity#onSaveInstanceState FragmentActivity.onSaveInstanceState()}
     * (and prior to a following {@link FragmentActivity#onStart FragmentActivity.onStart}
     * or {@link FragmentActivity#onResume FragmentActivity.onResume()}, you will get an error.
     * This is because the framework takes care of saving your current fragments
     * in the state, and if changes are made after the state is saved then they
     * will be lost.</p>
     */
    public abstract FragmentTransaction beginTransaction();

    /**
     * @hide -- remove once prebuilts are in.
     * @deprecated
     */
    @RestrictTo(LIBRARY_GROUP)
    @Deprecated
    public FragmentTransaction openTransaction() {
        return beginTransaction();
    }

    /**
     * After a {@link FragmentTransaction} is committed with
     * {@link FragmentTransaction#commit FragmentTransaction.commit()}, it
     * is scheduled to be executed asynchronously on the process's main thread.
     * If you want to immediately executing any such pending operations, you
     * can call this function (only from the main thread) to do so.  Note that
     * all callbacks and other related behavior will be done from within this
     * call, so be careful about where this is called from.
     *
     * <p>If you are committing a single transaction that does not modify the
     * fragment back stack, strongly consider using
     * {@link FragmentTransaction#commitNow()} instead. This can help avoid
     * unwanted side effects when other code in your app has pending committed
     * transactions that expect different timing.</p>
     * <p>
     * This also forces the start of any postponed Transactions where
     * {@link Fragment#postponeEnterTransition()} has been called.
     *
     * @return Returns true if there were any pending transactions to be
     * executed.
     */
    public abstract boolean executePendingTransactions();

    /**
     * Finds a fragment that was identified by the given id either when inflated
     * from XML or as the container ID when added in a transaction.  This first
     * searches through fragments that are currently added to the manager's
     * activity; if no such fragment is found, then all fragments currently
     * on the back stack associated with this ID are searched.
     * @return The fragment if found or null otherwise.
     */
    public abstract Fragment findFragmentById(@IdRes int id);

    /**
     * Finds a fragment that was identified by the given tag either when inflated
     * from XML or as supplied when added in a transaction.  This first
     * searches through fragments that are currently added to the manager's
     * activity; if no such fragment is found, then all fragments currently
     * on the back stack are searched.
     * @return The fragment if found or null otherwise.
     */
    public abstract Fragment findFragmentByTag(String tag);

    /**
     * Flag for {@link #popBackStack(String, int)}
     * and {@link #popBackStack(int, int)}: If set, and the name or ID of
     * a back stack entry has been supplied, then all matching entries will
     * be consumed until one that doesn't match is found or the bottom of
     * the stack is reached.  Otherwise, all entries up to but not including that entry
     * will be removed.
     */
    public static final int POP_BACK_STACK_INCLUSIVE = 1<<0;

    /**
     * Pop the top state off the back stack.  Returns true if there was one
     * to pop, else false.  This function is asynchronous -- it enqueues the
     * request to pop, but the action will not be performed until the application
     * returns to its event loop.
     */
    public abstract void popBackStack();

    /**
     * Like {@link #popBackStack()}, but performs the operation immediately
     * inside of the call.  This is like calling {@link #executePendingTransactions()}
     * afterwards without forcing the start of postponed Transactions.
     * @return Returns true if there was something popped, else false.
     */
    public abstract boolean popBackStackImmediate();

    /**
     * Pop the last fragment transition from the manager's fragment
     * back stack.  If there is nothing to pop, false is returned.
     * This function is asynchronous -- it enqueues the
     * request to pop, but the action will not be performed until the application
     * returns to its event loop.
     *
     * @param name If non-null, this is the name of a previous back state
     * to look for; if found, all states up to that state will be popped.  The
     * {@link #POP_BACK_STACK_INCLUSIVE} flag can be used to control whether
     * the named state itself is popped. If null, only the top state is popped.
     * @param flags Either 0 or {@link #POP_BACK_STACK_INCLUSIVE}.
     */
    public abstract void popBackStack(String name, int flags);

    /**
     * Like {@link #popBackStack(String, int)}, but performs the operation immediately
     * inside of the call.  This is like calling {@link #executePendingTransactions()}
     * afterwards without forcing the start of postponed Transactions.
     * @return Returns true if there was something popped, else false.
     */
    public abstract boolean popBackStackImmediate(String name, int flags);

    /**
     * Pop all back stack states up to the one with the given identifier.
     * This function is asynchronous -- it enqueues the
     * request to pop, but the action will not be performed until the application
     * returns to its event loop.
     *
     * @param id Identifier of the stated to be popped. If no identifier exists,
     * false is returned.
     * The identifier is the number returned by
     * {@link FragmentTransaction#commit() FragmentTransaction.commit()}.  The
     * {@link #POP_BACK_STACK_INCLUSIVE} flag can be used to control whether
     * the named state itself is popped.
     * @param flags Either 0 or {@link #POP_BACK_STACK_INCLUSIVE}.
     */
    public abstract void popBackStack(int id, int flags);

    /**
     * Like {@link #popBackStack(int, int)}, but performs the operation immediately
     * inside of the call.  This is like calling {@link #executePendingTransactions()}
     * afterwards without forcing the start of postponed Transactions.
     * @return Returns true if there was something popped, else false.
     */
    public abstract boolean popBackStackImmediate(int id, int flags);

    /**
     * Return the number of entries currently in the back stack.
     */
    public abstract int getBackStackEntryCount();

    /**
     * Return the BackStackEntry at index <var>index</var> in the back stack;
     * entries start index 0 being the bottom of the stack.
     */
    public abstract BackStackEntry getBackStackEntryAt(int index);

    /**
     * Add a new listener for changes to the fragment back stack.
     */
    public abstract void addOnBackStackChangedListener(OnBackStackChangedListener listener);

    /**
     * Remove a listener that was previously added with
     * {@link #addOnBackStackChangedListener(OnBackStackChangedListener)}.
     */
    public abstract void removeOnBackStackChangedListener(OnBackStackChangedListener listener);

    /**
     * Put a reference to a fragment in a Bundle.  This Bundle can be
     * persisted as saved state, and when later restoring
     * {@link #getFragment(Bundle, String)} will return the current
     * instance of the same fragment.
     *
     * @param bundle The bundle in which to put the fragment reference.
     * @param key The name of the entry in the bundle.
     * @param fragment The Fragment whose reference is to be stored.
     */
    public abstract void putFragment(Bundle bundle, String key, Fragment fragment);

    /**
     * Retrieve the current Fragment instance for a reference previously
     * placed with {@link #putFragment(Bundle, String, Fragment)}.
     *
     * @param bundle The bundle from which to retrieve the fragment reference.
     * @param key The name of the entry in the bundle.
     * @return Returns the current Fragment instance that is associated with
     * the given reference.
     */
    public abstract Fragment getFragment(Bundle bundle, String key);

    /**
     * Get a list of all fragments that have been added to the fragment manager.
     *
     * @return The list of all fragments or null if none.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public abstract List<Fragment> getFragments();

    /**
     * Save the current instance state of the given Fragment.  This can be
     * used later when creating a new instance of the Fragment and adding
     * it to the fragment manager, to have it create itself to match the
     * current state returned here.  Note that there are limits on how
     * this can be used:
     *
     * <ul>
     * <li>The Fragment must currently be attached to the FragmentManager.
     * <li>A new Fragment created using this saved state must be the same class
     * type as the Fragment it was created from.
     * <li>The saved state can not contain dependencies on other fragments --
     * that is it can't use {@link #putFragment(Bundle, String, Fragment)} to
     * store a fragment reference because that reference may not be valid when
     * this saved state is later used.  Likewise the Fragment's target and
     * result code are not included in this state.
     * </ul>
     *
     * @param f The Fragment whose state is to be saved.
     * @return The generated state.  This will be null if there was no
     * interesting state created by the fragment.
     */
    public abstract Fragment.SavedState saveFragmentInstanceState(Fragment f);

    /**
     * Returns true if the final {@link android.app.Activity#onDestroy() Activity.onDestroy()}
     * call has been made on the FragmentManager's Activity, so this instance is now dead.
     */
    public abstract boolean isDestroyed();

    /**
     * Registers a {@link FragmentLifecycleCallbacks} to listen to fragment lifecycle events
     * happening in this FragmentManager. All registered callbacks will be automatically
     * unregistered when this FragmentManager is destroyed.
     *
     * @param cb Callbacks to register
     * @param recursive true to automatically register this callback for all child FragmentManagers
     */
    public abstract void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb,
            boolean recursive);

    /**
     * Unregisters a previously registered {@link FragmentLifecycleCallbacks}. If the callback
     * was not previously registered this call has no effect. All registered callbacks will be
     * automatically unregistered when this FragmentManager is destroyed.
     *
     * @param cb Callbacks to unregister
     */
    public abstract void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb);

    /**
     * Print the FragmentManager's state into the given stream.
     *
     * @param prefix Text to print at the front of each line.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer A PrintWriter to which the dump is to be set.
     * @param args Additional arguments to the dump request.
     */
    public abstract void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args);

    /**
     * Control whether the framework's internal fragment manager debugging
     * logs are turned on.  If enabled, you will see output in logcat as
     * the framework performs fragment operations.
     */
    public static void enableDebugLogging(boolean enabled) {
        FragmentManagerImpl.DEBUG = enabled;
    }

    /**
     * Callback interface for listening to fragment state changes that happen
     * within a given FragmentManager.
     */
    public abstract static class FragmentLifecycleCallbacks {
        /**
         * Called right before the fragment's {@link Fragment#onAttach(Context)} method is called.
         * This is a good time to inject any required dependencies for the fragment before any of
         * the fragment's lifecycle methods are invoked.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param context Context that the Fragment is being attached to
         */
        public void onFragmentPreAttached(FragmentManager fm, Fragment f, Context context) {}

        /**
         * Called after the fragment has been attached to its host. Its host will have had
         * <code>onAttachFragment</code> called before this call happens.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param context Context that the Fragment was attached to
         */
        public void onFragmentAttached(FragmentManager fm, Fragment f, Context context) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onCreate(Bundle)}. This will only happen once for any given
         * fragment instance, though the fragment may be attached and detached multiple times.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param savedInstanceState Saved instance bundle from a previous instance
         */
        public void onFragmentCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onActivityCreated(Bundle)}. This will only happen once for any given
         * fragment instance, though the fragment may be attached and detached multiple times.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param savedInstanceState Saved instance bundle from a previous instance
         */
        public void onFragmentActivityCreated(FragmentManager fm, Fragment f,
                Bundle savedInstanceState) {}

        /**
         * Called after the fragment has returned a non-null view from the FragmentManager's
         * request to {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment that created and owns the view
         * @param v View returned by the fragment
         * @param savedInstanceState Saved instance bundle from a previous instance
         */
        public void onFragmentViewCreated(FragmentManager fm, Fragment f, View v,
                Bundle savedInstanceState) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onStart()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentStarted(FragmentManager fm, Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onResume()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentResumed(FragmentManager fm, Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onPause()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentPaused(FragmentManager fm, Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onStop()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentStopped(FragmentManager fm, Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onSaveInstanceState(Bundle)}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param outState Saved state bundle for the fragment
         */
        public void onFragmentSaveInstanceState(FragmentManager fm, Fragment f, Bundle outState) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onDestroyView()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentViewDestroyed(FragmentManager fm, Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onDestroy()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentDestroyed(FragmentManager fm, Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onDetach()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentDetached(FragmentManager fm, Fragment f) {}
    }
}

final class FragmentManagerState implements Parcelable {
    FragmentState[] mActive;
    int[] mAdded;
    BackStackState[] mBackStack;

    public FragmentManagerState() {
    }

    public FragmentManagerState(Parcel in) {
        mActive = in.createTypedArray(FragmentState.CREATOR);
        mAdded = in.createIntArray();
        mBackStack = in.createTypedArray(BackStackState.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(mActive, flags);
        dest.writeIntArray(mAdded);
        dest.writeTypedArray(mBackStack, flags);
    }

    public static final Parcelable.Creator<FragmentManagerState> CREATOR
            = new Parcelable.Creator<FragmentManagerState>() {
        @Override
        public FragmentManagerState createFromParcel(Parcel in) {
            return new FragmentManagerState(in);
        }

        @Override
        public FragmentManagerState[] newArray(int size) {
            return new FragmentManagerState[size];
        }
    };
}

/**
 * Container for fragments associated with an activity.
 */
final class FragmentManagerImpl extends FragmentManager implements LayoutInflaterFactory {
    static boolean DEBUG = false;
    static final String TAG = "FragmentManager";

    static final boolean HONEYCOMB = android.os.Build.VERSION.SDK_INT >= 11;

    static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    static final String TARGET_STATE_TAG = "android:target_state";
    static final String VIEW_STATE_TAG = "android:view_state";
    static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";

    static class AnimateOnHWLayerIfNeededListener implements AnimationListener {
        private AnimationListener mOriginalListener;
        private boolean mShouldRunOnHWLayer;
        View mView;

        public AnimateOnHWLayerIfNeededListener(final View v, Animation anim) {
            if (v == null || anim == null) {
                return;
            }
            mView = v;
        }

        public AnimateOnHWLayerIfNeededListener(final View v, Animation anim,
                AnimationListener listener) {
            if (v == null || anim == null) {
                return;
            }
            mOriginalListener = listener;
            mView = v;
            mShouldRunOnHWLayer = true;
        }

        @Override
        @CallSuper
        public void onAnimationStart(Animation animation) {
            if (mOriginalListener != null) {
                mOriginalListener.onAnimationStart(animation);
            }
        }

        @Override
        @CallSuper
        public void onAnimationEnd(Animation animation) {
            if (mView != null && mShouldRunOnHWLayer) {
                // If we're attached to a window, assume we're in the normal performTraversals
                // drawing path for Animations running. It's not safe to change the layer type
                // during drawing, so post it to the View to run later. If we're not attached
                // or we're running on N and above, post it to the view. If we're not on N and
                // not attached, do it right now since existing platform versions don't run the
                // hwui renderer for detached views off the UI thread making changing layer type
                // safe, but posting may not be.
                // Prior to N posting to a detached view from a non-Looper thread could cause
                // leaks, since the thread-local run queue on a non-Looper thread would never
                // be flushed.
                if (ViewCompat.isAttachedToWindow(mView) || BuildCompat.isAtLeastN()) {
                    mView.post(new Runnable() {
                        @Override
                        public void run() {
                            ViewCompat.setLayerType(mView, ViewCompat.LAYER_TYPE_NONE, null);
                        }
                    });
                } else {
                    ViewCompat.setLayerType(mView, ViewCompat.LAYER_TYPE_NONE, null);
                }
            }
            if (mOriginalListener != null) {
                mOriginalListener.onAnimationEnd(animation);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            if (mOriginalListener != null) {
                mOriginalListener.onAnimationRepeat(animation);
            }
        }
    }

    ArrayList<OpGenerator> mPendingActions;
    Runnable[] mTmpActions;
    boolean mExecutingActions;

    ArrayList<Fragment> mActive;
    ArrayList<Fragment> mAdded;
    ArrayList<Integer> mAvailIndices;
    ArrayList<BackStackRecord> mBackStack;
    ArrayList<Fragment> mCreatedMenus;

    // Must be accessed while locked.
    ArrayList<BackStackRecord> mBackStackIndices;
    ArrayList<Integer> mAvailBackStackIndices;

    ArrayList<OnBackStackChangedListener> mBackStackChangeListeners;
    private CopyOnWriteArrayList<Pair<FragmentLifecycleCallbacks, Boolean>> mLifecycleCallbacks;

    int mCurState = Fragment.INITIALIZING;
    FragmentHostCallback mHost;
    FragmentContainer mContainer;
    Fragment mParent;

    static Field sAnimationListenerField = null;

    boolean mNeedMenuInvalidate;
    boolean mStateSaved;
    boolean mDestroyed;
    String mNoTransactionsBecause;
    boolean mHavePendingDeferredStart;

    // Temporary vars for optimizing execution of BackStackRecords:
    ArrayList<BackStackRecord> mTmpRecords;
    ArrayList<Boolean> mTmpIsPop;
    ArrayList<Fragment> mTmpAddedFragments;

    // Temporary vars for state save and restore.
    Bundle mStateBundle = null;
    SparseArray<Parcelable> mStateArray = null;

    // Postponed transactions.
    ArrayList<StartEnterTransitionListener> mPostponedTransactions;

    Runnable mExecCommit = new Runnable() {
        @Override
        public void run() {
            execPendingActions();
        }
    };

    static boolean modifiesAlpha(Animation anim) {
        if (anim instanceof AlphaAnimation) {
            return true;
        } else if (anim instanceof AnimationSet) {
            List<Animation> anims = ((AnimationSet) anim).getAnimations();
            for (int i = 0; i < anims.size(); i++) {
                if (anims.get(i) instanceof AlphaAnimation) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean shouldRunOnHWLayer(View v, Animation anim) {
        return Build.VERSION.SDK_INT >= 19
                && ViewCompat.getLayerType(v) == ViewCompat.LAYER_TYPE_NONE
                && ViewCompat.hasOverlappingRendering(v)
                && modifiesAlpha(anim);
    }

    private void throwException(RuntimeException ex) {
        Log.e(TAG, ex.getMessage());
        Log.e(TAG, "Activity state:");
        LogWriter logw = new LogWriter(TAG);
        PrintWriter pw = new PrintWriter(logw);
        if (mHost != null) {
            try {
                mHost.onDump("  ", null, pw, new String[] { });
            } catch (Exception e) {
                Log.e(TAG, "Failed dumping state", e);
            }
        } else {
            try {
                dump("  ", null, pw, new String[] { });
            } catch (Exception e) {
                Log.e(TAG, "Failed dumping state", e);
            }
        }
        throw ex;
    }

    @Override
    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }

    @Override
    public boolean executePendingTransactions() {
        boolean updates = execPendingActions();
        forcePostponedTransactions();
        return updates;
    }

    @Override
    public void popBackStack() {
        enqueueAction(new PopBackStackState(null, -1, 0), false);
    }

    @Override
    public boolean popBackStackImmediate() {
        checkStateLoss();
        return popBackStackImmediate(null, -1, 0);
    }

    @Override
    public void popBackStack(final String name, final int flags) {
        enqueueAction(new PopBackStackState(name, -1, flags), false);
    }

    @Override
    public boolean popBackStackImmediate(String name, int flags) {
        checkStateLoss();
        return popBackStackImmediate(name, -1, flags);
    }

    @Override
    public void popBackStack(final int id, final int flags) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        enqueueAction(new PopBackStackState(null, id, flags), false);
    }

    @Override
    public boolean popBackStackImmediate(int id, int flags) {
        checkStateLoss();
        execPendingActions();
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        return popBackStackImmediate(null, id, flags);
    }

    /**
     * Used by all public popBackStackImmediate methods, this executes pending transactions and
     * returns true if the pop action did anything, regardless of what other pending
     * transactions did.
     *
     * @return true if the pop operation did anything or false otherwise.
     */
    private boolean popBackStackImmediate(String name, int id, int flags) {
        execPendingActions();
        ensureExecReady(true);

        boolean executePop = popBackStackState(mTmpRecords, mTmpIsPop, name, id, flags);
        if (executePop) {
            mExecutingActions = true;
            try {
                optimizeAndExecuteOps(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }

        doPendingDeferredStart();
        return executePop;
    }

    @Override
    public int getBackStackEntryCount() {
        return mBackStack != null ? mBackStack.size() : 0;
    }

    @Override
    public BackStackEntry getBackStackEntryAt(int index) {
        return mBackStack.get(index);
    }

    @Override
    public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners == null) {
            mBackStackChangeListeners = new ArrayList<OnBackStackChangedListener>();
        }
        mBackStackChangeListeners.add(listener);
    }

    @Override
    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners != null) {
            mBackStackChangeListeners.remove(listener);
        }
    }

    @Override
    public void putFragment(Bundle bundle, String key, Fragment fragment) {
        if (fragment.mIndex < 0) {
            throwException(new IllegalStateException("Fragment " + fragment
                    + " is not currently in the FragmentManager"));
        }
        bundle.putInt(key, fragment.mIndex);
    }

    @Override
    public Fragment getFragment(Bundle bundle, String key) {
        int index = bundle.getInt(key, -1);
        if (index == -1) {
            return null;
        }
        if (index >= mActive.size()) {
            throwException(new IllegalStateException("Fragment no longer exists for key "
                    + key + ": index " + index));
        }
        Fragment f = mActive.get(index);
        if (f == null) {
            throwException(new IllegalStateException("Fragment no longer exists for key "
                    + key + ": index " + index));
        }
        return f;
    }

    @Override
    public List<Fragment> getFragments() {
        return mActive;
    }

    @Override
    public Fragment.SavedState saveFragmentInstanceState(Fragment fragment) {
        if (fragment.mIndex < 0) {
            throwException( new IllegalStateException("Fragment " + fragment
                    + " is not currently in the FragmentManager"));
        }
        if (fragment.mState > Fragment.INITIALIZING) {
            Bundle result = saveFragmentBasicState(fragment);
            return result != null ? new Fragment.SavedState(result) : null;
        }
        return null;
    }

    @Override
    public boolean isDestroyed() {
        return mDestroyed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FragmentManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        if (mParent != null) {
            DebugUtils.buildShortClassTag(mParent, sb);
        } else {
            DebugUtils.buildShortClassTag(mHost, sb);
        }
        sb.append("}}");
        return sb.toString();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        String innerPrefix = prefix + "    ";

        int N;
        if (mActive != null) {
            N = mActive.size();
            if (N > 0) {
                writer.print(prefix); writer.print("Active Fragments in ");
                        writer.print(Integer.toHexString(System.identityHashCode(this)));
                        writer.println(":");
                for (int i=0; i<N; i++) {
                    Fragment f = mActive.get(i);
                    writer.print(prefix); writer.print("  #"); writer.print(i);
                            writer.print(": "); writer.println(f);
                    if (f != null) {
                        f.dump(innerPrefix, fd, writer, args);
                    }
                }
            }
        }

        if (mAdded != null) {
            N = mAdded.size();
            if (N > 0) {
                writer.print(prefix); writer.println("Added Fragments:");
                for (int i=0; i<N; i++) {
                    Fragment f = mAdded.get(i);
                    writer.print(prefix); writer.print("  #"); writer.print(i);
                            writer.print(": "); writer.println(f.toString());
                }
            }
        }

        if (mCreatedMenus != null) {
            N = mCreatedMenus.size();
            if (N > 0) {
                writer.print(prefix); writer.println("Fragments Created Menus:");
                for (int i=0; i<N; i++) {
                    Fragment f = mCreatedMenus.get(i);
                    writer.print(prefix); writer.print("  #"); writer.print(i);
                            writer.print(": "); writer.println(f.toString());
                }
            }
        }

        if (mBackStack != null) {
            N = mBackStack.size();
            if (N > 0) {
                writer.print(prefix); writer.println("Back Stack:");
                for (int i=0; i<N; i++) {
                    BackStackRecord bs = mBackStack.get(i);
                    writer.print(prefix); writer.print("  #"); writer.print(i);
                            writer.print(": "); writer.println(bs.toString());
                    bs.dump(innerPrefix, fd, writer, args);
                }
            }
        }

        synchronized (this) {
            if (mBackStackIndices != null) {
                N = mBackStackIndices.size();
                if (N > 0) {
                    writer.print(prefix); writer.println("Back Stack Indices:");
                    for (int i=0; i<N; i++) {
                        BackStackRecord bs = mBackStackIndices.get(i);
                        writer.print(prefix); writer.print("  #"); writer.print(i);
                                writer.print(": "); writer.println(bs);
                    }
                }
            }

            if (mAvailBackStackIndices != null && mAvailBackStackIndices.size() > 0) {
                writer.print(prefix); writer.print("mAvailBackStackIndices: ");
                        writer.println(Arrays.toString(mAvailBackStackIndices.toArray()));
            }
        }

        if (mPendingActions != null) {
            N = mPendingActions.size();
            if (N > 0) {
                writer.print(prefix); writer.println("Pending Actions:");
                for (int i=0; i<N; i++) {
                    OpGenerator r = mPendingActions.get(i);
                    writer.print(prefix); writer.print("  #"); writer.print(i);
                            writer.print(": "); writer.println(r);
                }
            }
        }

        writer.print(prefix); writer.println("FragmentManager misc state:");
        writer.print(prefix); writer.print("  mHost="); writer.println(mHost);
        writer.print(prefix); writer.print("  mContainer="); writer.println(mContainer);
        if (mParent != null) {
            writer.print(prefix); writer.print("  mParent="); writer.println(mParent);
        }
        writer.print(prefix); writer.print("  mCurState="); writer.print(mCurState);
                writer.print(" mStateSaved="); writer.print(mStateSaved);
                writer.print(" mDestroyed="); writer.println(mDestroyed);
        if (mNeedMenuInvalidate) {
            writer.print(prefix); writer.print("  mNeedMenuInvalidate=");
                    writer.println(mNeedMenuInvalidate);
        }
        if (mNoTransactionsBecause != null) {
            writer.print(prefix); writer.print("  mNoTransactionsBecause=");
                    writer.println(mNoTransactionsBecause);
        }
        if (mAvailIndices != null && mAvailIndices.size() > 0) {
            writer.print(prefix); writer.print("  mAvailIndices: ");
                    writer.println(Arrays.toString(mAvailIndices.toArray()));
        }
    }

    static final Interpolator DECELERATE_QUINT = new DecelerateInterpolator(2.5f);
    static final Interpolator DECELERATE_CUBIC = new DecelerateInterpolator(1.5f);
    static final Interpolator ACCELERATE_QUINT = new AccelerateInterpolator(2.5f);
    static final Interpolator ACCELERATE_CUBIC = new AccelerateInterpolator(1.5f);

    static final int ANIM_DUR = 220;

    static Animation makeOpenCloseAnimation(Context context, float startScale,
            float endScale, float startAlpha, float endAlpha) {
        AnimationSet set = new AnimationSet(false);
        ScaleAnimation scale = new ScaleAnimation(startScale, endScale, startScale, endScale,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        scale.setInterpolator(DECELERATE_QUINT);
        scale.setDuration(ANIM_DUR);
        set.addAnimation(scale);
        AlphaAnimation alpha = new AlphaAnimation(startAlpha, endAlpha);
        alpha.setInterpolator(DECELERATE_CUBIC);
        alpha.setDuration(ANIM_DUR);
        set.addAnimation(alpha);
        return set;
    }

    static Animation makeFadeAnimation(Context context, float start, float end) {
        AlphaAnimation anim = new AlphaAnimation(start, end);
        anim.setInterpolator(DECELERATE_CUBIC);
        anim.setDuration(ANIM_DUR);
        return anim;
    }

    Animation loadAnimation(Fragment fragment, int transit, boolean enter,
            int transitionStyle) {
        Animation animObj = fragment.onCreateAnimation(transit, enter, fragment.getNextAnim());
        if (animObj != null) {
            return animObj;
        }

        if (fragment.getNextAnim() != 0) {
            Animation anim = AnimationUtils.loadAnimation(mHost.getContext(),
                    fragment.getNextAnim());
            if (anim != null) {
                return anim;
            }
        }

        if (transit == 0) {
            return null;
        }

        int styleIndex = transitToStyleIndex(transit, enter);
        if (styleIndex < 0) {
            return null;
        }

        switch (styleIndex) {
            case ANIM_STYLE_OPEN_ENTER:
                return makeOpenCloseAnimation(mHost.getContext(), 1.125f, 1.0f, 0, 1);
            case ANIM_STYLE_OPEN_EXIT:
                return makeOpenCloseAnimation(mHost.getContext(), 1.0f, .975f, 1, 0);
            case ANIM_STYLE_CLOSE_ENTER:
                return makeOpenCloseAnimation(mHost.getContext(), .975f, 1.0f, 0, 1);
            case ANIM_STYLE_CLOSE_EXIT:
                return makeOpenCloseAnimation(mHost.getContext(), 1.0f, 1.075f, 1, 0);
            case ANIM_STYLE_FADE_ENTER:
                return makeFadeAnimation(mHost.getContext(), 0, 1);
            case ANIM_STYLE_FADE_EXIT:
                return makeFadeAnimation(mHost.getContext(), 1, 0);
        }

        if (transitionStyle == 0 && mHost.onHasWindowAnimations()) {
            transitionStyle = mHost.onGetWindowAnimations();
        }
        if (transitionStyle == 0) {
            return null;
        }

        //TypedArray attrs = mActivity.obtainStyledAttributes(transitionStyle,
        //        com.android.internal.R.styleable.FragmentAnimation);
        //int anim = attrs.getResourceId(styleIndex, 0);
        //attrs.recycle();

        //if (anim == 0) {
        //    return null;
        //}

        //return AnimatorInflater.loadAnimator(mActivity, anim);
        return null;
    }

    public void performPendingDeferredStart(Fragment f) {
        if (f.mDeferStart) {
            if (mExecutingActions) {
                // Wait until we're done executing our pending transactions
                mHavePendingDeferredStart = true;
                return;
            }
            f.mDeferStart = false;
            moveToState(f, mCurState, 0, 0, false);
        }
    }

    /**
     * Sets the to be animated view on hardware layer during the animation. Note
     * that calling this will replace any existing animation listener on the animation
     * with a new one, as animations do not support more than one listeners. Therefore,
     * animations that already have listeners should do the layer change operations
     * in their existing listeners, rather than calling this function.
     */
    private void setHWLayerAnimListenerIfAlpha(final View v, Animation anim) {
        if (v == null || anim == null) {
            return;
        }
        if (shouldRunOnHWLayer(v, anim)) {
            AnimationListener originalListener = null;
            try {
                if (sAnimationListenerField == null) {
                    sAnimationListenerField = Animation.class.getDeclaredField("mListener");
                    sAnimationListenerField.setAccessible(true);
                }
                originalListener = (AnimationListener) sAnimationListenerField.get(anim);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "No field with the name mListener is found in Animation class", e);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Cannot access Animation's mListener field", e);
            }
            // If there's already a listener set on the animation, we need wrap the new listener
            // around the existing listener, so that they will both get animation listener
            // callbacks.
            ViewCompat.setLayerType(v, ViewCompat.LAYER_TYPE_HARDWARE, null);
            anim.setAnimationListener(new AnimateOnHWLayerIfNeededListener(v, anim,
                    originalListener));
        }
    }

    boolean isStateAtLeast(int state) {
        return mCurState >= state;
    }

    void moveToState(Fragment f, int newState, int transit, int transitionStyle,
            boolean keepActive) {
        // Fragments that are not currently added will sit in the onCreate() state.
        if ((!f.mAdded || f.mDetached) && newState > Fragment.CREATED) {
            newState = Fragment.CREATED;
        }
        if (f.mRemoving && newState > f.mState) {
            // While removing a fragment, we can't change it to a higher state.
            newState = f.mState;
        }
        // Defer start if requested; don't allow it to move to STARTED or higher
        // if it's not already started.
        if (f.mDeferStart && f.mState < Fragment.STARTED && newState > Fragment.STOPPED) {
            newState = Fragment.STOPPED;
        }
        if (f.mState < newState) {
            // For fragments that are created from a layout, when restoring from
            // state we don't want to allow them to be created until they are
            // being reloaded from the layout.
            if (f.mFromLayout && !f.mInLayout) {
                return;
            }
            if (f.getAnimatingAway() != null) {
                // The fragment is currently being animated...  but!  Now we
                // want to move our state back up.  Give up on waiting for the
                // animation, move to whatever the final state should be once
                // the animation is done, and then we can proceed from there.
                f.setAnimatingAway(null);
                moveToState(f, f.getStateAfterAnimating(), 0, 0, true);
            }
            switch (f.mState) {
                case Fragment.INITIALIZING:
                    if (DEBUG) Log.v(TAG, "moveto CREATED: " + f);
                    if (f.mSavedFragmentState != null) {
                        f.mSavedFragmentState.setClassLoader(mHost.getContext().getClassLoader());
                        f.mSavedViewState = f.mSavedFragmentState.getSparseParcelableArray(
                                FragmentManagerImpl.VIEW_STATE_TAG);
                        f.mTarget = getFragment(f.mSavedFragmentState,
                                FragmentManagerImpl.TARGET_STATE_TAG);
                        if (f.mTarget != null) {
                            f.mTargetRequestCode = f.mSavedFragmentState.getInt(
                                    FragmentManagerImpl.TARGET_REQUEST_CODE_STATE_TAG, 0);
                        }
                        f.mUserVisibleHint = f.mSavedFragmentState.getBoolean(
                                FragmentManagerImpl.USER_VISIBLE_HINT_TAG, true);
                        if (!f.mUserVisibleHint) {
                            f.mDeferStart = true;
                            if (newState > Fragment.STOPPED) {
                                newState = Fragment.STOPPED;
                            }
                        }
                    }
                    f.mHost = mHost;
                    f.mParentFragment = mParent;
                    f.mFragmentManager = mParent != null
                            ? mParent.mChildFragmentManager : mHost.getFragmentManagerImpl();
                    dispatchOnFragmentPreAttached(f, mHost.getContext(), false);
                    f.mCalled = false;
                    f.onAttach(mHost.getContext());
                    if (!f.mCalled) {
                        throw new SuperNotCalledException("Fragment " + f
                                + " did not call through to super.onAttach()");
                    }
                    if (f.mParentFragment == null) {
                        mHost.onAttachFragment(f);
                    } else {
                        f.mParentFragment.onAttachFragment(f);
                    }
                    dispatchOnFragmentAttached(f, mHost.getContext(), false);

                    if (!f.mRetaining) {
                        f.performCreate(f.mSavedFragmentState);
                        dispatchOnFragmentCreated(f, f.mSavedFragmentState, false);
                    } else {
                        f.restoreChildFragmentState(f.mSavedFragmentState);
                        f.mState = Fragment.CREATED;
                    }
                    f.mRetaining = false;
                    if (f.mFromLayout) {
                        // For fragments that are part of the content view
                        // layout, we need to instantiate the view immediately
                        // and the inflater will take care of adding it.
                        f.mView = f.performCreateView(f.getLayoutInflater(
                                f.mSavedFragmentState), null, f.mSavedFragmentState);
                        if (f.mView != null) {
                            f.mInnerView = f.mView;
                            if (Build.VERSION.SDK_INT >= 11) {
                                ViewCompat.setSaveFromParentEnabled(f.mView, false);
                            } else {
                                f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                            }
                            if (f.mHidden) f.mView.setVisibility(View.GONE);
                            f.onViewCreated(f.mView, f.mSavedFragmentState);
                            dispatchOnFragmentViewCreated(f, f.mView, f.mSavedFragmentState, false);
                        } else {
                            f.mInnerView = null;
                        }
                    }
                case Fragment.CREATED:
                    if (newState > Fragment.CREATED) {
                        if (DEBUG) Log.v(TAG, "moveto ACTIVITY_CREATED: " + f);
                        if (!f.mFromLayout) {
                            ViewGroup container = null;
                            if (f.mContainerId != 0) {
                                if (f.mContainerId == View.NO_ID) {
                                    throwException(new IllegalArgumentException(
                                            "Cannot create fragment "
                                                    + f
                                                    + " for a container view with no id"));
                                }
                                container = (ViewGroup) mContainer.onFindViewById(f.mContainerId);
                                if (container == null && !f.mRestored) {
                                    String resName;
                                    try {
                                        resName = f.getResources().getResourceName(f.mContainerId);
                                    } catch (NotFoundException e) {
                                        resName = "unknown";
                                    }
                                    throwException(new IllegalArgumentException(
                                            "No view found for id 0x"
                                            + Integer.toHexString(f.mContainerId) + " ("
                                            + resName
                                            + ") for fragment " + f));
                                }
                            }
                            f.mContainer = container;
                            f.mView = f.performCreateView(f.getLayoutInflater(
                                    f.mSavedFragmentState), container, f.mSavedFragmentState);
                            if (f.mView != null) {
                                f.mInnerView = f.mView;
                                if (Build.VERSION.SDK_INT >= 11) {
                                    ViewCompat.setSaveFromParentEnabled(f.mView, false);
                                } else {
                                    f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                                }
                                if (container != null) {
                                    container.addView(f.mView);
                                }
                                if (f.mHidden) {
                                    f.mView.setVisibility(View.GONE);
                                }
                                f.onViewCreated(f.mView, f.mSavedFragmentState);
                                dispatchOnFragmentViewCreated(f, f.mView, f.mSavedFragmentState,
                                        false);
                                // Only animate the view if it is visible. This is done after
                                // dispatchOnFragmentViewCreated in case visibility is changed
                                f.mIsNewlyAdded = (f.mView.getVisibility() == View.VISIBLE)
                                        && f.mContainer != null;
                            } else {
                                f.mInnerView = null;
                            }
                        }

                        f.performActivityCreated(f.mSavedFragmentState);
                        dispatchOnFragmentActivityCreated(f, f.mSavedFragmentState, false);
                        if (f.mView != null) {
                            f.restoreViewState(f.mSavedFragmentState);
                        }
                        f.mSavedFragmentState = null;
                    }
                case Fragment.ACTIVITY_CREATED:
                    if (newState > Fragment.ACTIVITY_CREATED) {
                        f.mState = Fragment.STOPPED;
                    }
                case Fragment.STOPPED:
                    if (newState > Fragment.STOPPED) {
                        if (DEBUG) Log.v(TAG, "moveto STARTED: " + f);
                        f.performStart();
                        dispatchOnFragmentStarted(f, false);
                    }
                case Fragment.STARTED:
                    if (newState > Fragment.STARTED) {
                        if (DEBUG) Log.v(TAG, "moveto RESUMED: " + f);
                        f.performResume();
                        dispatchOnFragmentResumed(f, false);
                        f.mSavedFragmentState = null;
                        f.mSavedViewState = null;
                    }
            }
        } else if (f.mState > newState) {
            switch (f.mState) {
                case Fragment.RESUMED:
                    if (newState < Fragment.RESUMED) {
                        if (DEBUG) Log.v(TAG, "movefrom RESUMED: " + f);
                        f.performPause();
                        dispatchOnFragmentPaused(f, false);
                    }
                case Fragment.STARTED:
                    if (newState < Fragment.STARTED) {
                        if (DEBUG) Log.v(TAG, "movefrom STARTED: " + f);
                        f.performStop();
                        dispatchOnFragmentStopped(f, false);
                    }
                case Fragment.STOPPED:
                    if (newState < Fragment.STOPPED) {
                        if (DEBUG) Log.v(TAG, "movefrom STOPPED: " + f);
                        f.performReallyStop();
                    }
                case Fragment.ACTIVITY_CREATED:
                    if (newState < Fragment.ACTIVITY_CREATED) {
                        if (DEBUG) Log.v(TAG, "movefrom ACTIVITY_CREATED: " + f);
                        if (f.mView != null) {
                            // Need to save the current view state if not
                            // done already.
                            if (mHost.onShouldSaveFragmentState(f) && f.mSavedViewState == null) {
                                saveFragmentViewState(f);
                            }
                        }
                        f.performDestroyView();
                        dispatchOnFragmentViewDestroyed(f, false);
                        if (f.mView != null && f.mContainer != null) {
                            Animation anim = null;
                            if (mCurState > Fragment.INITIALIZING && !mDestroyed
                                    && f.mView.getVisibility() == View.VISIBLE
                                    && f.mPostponedAlpha >= 0) {
                                anim = loadAnimation(f, transit, false,
                                        transitionStyle);
                            }
                            f.mPostponedAlpha = 0;
                            if (anim != null) {
                                final Fragment fragment = f;
                                f.setAnimatingAway(f.mView);
                                f.setStateAfterAnimating(newState);
                                final View viewToAnimate = f.mView;
                                anim.setAnimationListener(new AnimateOnHWLayerIfNeededListener(
                                        viewToAnimate, anim) {
                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        super.onAnimationEnd(animation);
                                        if (fragment.getAnimatingAway() != null) {
                                            fragment.setAnimatingAway(null);
                                            moveToState(fragment, fragment.getStateAfterAnimating(),
                                                    0, 0, false);
                                        }
                                    }
                                });
                                f.mView.startAnimation(anim);
                            }
                            f.mContainer.removeView(f.mView);
                        }
                        f.mContainer = null;
                        f.mView = null;
                        f.mInnerView = null;
                    }
                case Fragment.CREATED:
                    if (newState < Fragment.CREATED) {
                        if (mDestroyed) {
                            if (f.getAnimatingAway() != null) {
                                // The fragment's containing activity is
                                // being destroyed, but this fragment is
                                // currently animating away.  Stop the
                                // animation right now -- it is not needed,
                                // and we can't wait any more on destroying
                                // the fragment.
                                View v = f.getAnimatingAway();
                                f.setAnimatingAway(null);
                                v.clearAnimation();
                            }
                        }
                        if (f.getAnimatingAway() != null) {
                            // We are waiting for the fragment's view to finish
                            // animating away.  Just make a note of the state
                            // the fragment now should move to once the animation
                            // is done.
                            f.setStateAfterAnimating(newState);
                            newState = Fragment.CREATED;
                        } else {
                            if (DEBUG) Log.v(TAG, "movefrom CREATED: " + f);
                            if (!f.mRetaining) {
                                f.performDestroy();
                                dispatchOnFragmentDestroyed(f, false);
                            } else {
                                f.mState = Fragment.INITIALIZING;
                            }

                            f.performDetach();
                            dispatchOnFragmentDetached(f, false);
                            if (!keepActive) {
                                if (!f.mRetaining) {
                                    makeInactive(f);
                                } else {
                                    f.mHost = null;
                                    f.mParentFragment = null;
                                    f.mFragmentManager = null;
                                }
                            }
                        }
                    }
            }
        }

        if (f.mState != newState) {
            Log.w(TAG, "moveToState: Fragment state for " + f + " not updated inline; "
                    + "expected state " + newState + " found " + f.mState);
            f.mState = newState;
        }
    }

    void moveToState(Fragment f) {
        moveToState(f, mCurState, 0, 0, false);
    }

    /**
     * Fragments that have been shown or hidden don't have their visibility changed or
     * animations run during the {@link #showFragment(Fragment)} or {@link #hideFragment(Fragment)}
     * calls. After fragments are brought to their final state in
     * {@link #moveFragmentToExpectedState(Fragment)} the fragments that have been shown or
     * hidden must have their visibility changed and their animations started here.
     *
     * @param fragment The fragment with mHiddenChanged = true that should change its View's
     *                 visibility and start the show or hide animation.
     */
    void completeShowHideFragment(final Fragment fragment) {
        if (fragment.mView != null) {
            Animation anim = loadAnimation(fragment, fragment.getNextTransition(),
                    !fragment.mHidden, fragment.getNextTransitionStyle());
            if (anim != null) {
                setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                fragment.mView.startAnimation(anim);
                setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                anim.start();
            }
            final int visibility = fragment.mHidden && !fragment.isHideReplaced()
                    ? View.GONE
                    : View.VISIBLE;
            fragment.mView.setVisibility(visibility);
            if (fragment.isHideReplaced()) {
                fragment.setHideReplaced(false);
            }
        }
        if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) {
            mNeedMenuInvalidate = true;
        }
        fragment.mHiddenChanged = false;
        fragment.onHiddenChanged(fragment.mHidden);
    }

    /**
     * Moves a fragment to its expected final state or the fragment manager's state, depending
     * on whether the fragment manager's state is raised properly.
     *
     * @param f The fragment to change.
     */
    void moveFragmentToExpectedState(Fragment f) {
        if (f == null) {
            return;
        }
        int nextState = mCurState;
        if (f.mRemoving) {
            if (f.isInBackStack()) {
                nextState = Math.min(nextState, Fragment.CREATED);
            } else {
                nextState = Math.min(nextState, Fragment.INITIALIZING);
            }
        }
        moveToState(f, nextState, f.getNextTransition(), f.getNextTransitionStyle(), false);

        if (f.mView != null) {
            // Move the view if it is out of order
            Fragment underFragment = findFragmentUnder(f);
            if (underFragment != null) {
                final View underView = underFragment.mView;
                // make sure this fragment is in the right order.
                final ViewGroup container = f.mContainer;
                int underIndex = container.indexOfChild(underView);
                int viewIndex = container.indexOfChild(f.mView);
                if (viewIndex < underIndex) {
                    container.removeViewAt(viewIndex);
                    container.addView(f.mView, underIndex);
                }
            }
            if (f.mIsNewlyAdded && f.mContainer != null) {
                // Make it visible and run the animations
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    f.mView.setVisibility(View.VISIBLE);
                } else if (f.mPostponedAlpha > 0f) {
                    f.mView.setAlpha(f.mPostponedAlpha);
                }
                f.mPostponedAlpha = 0f;
                f.mIsNewlyAdded = false;
                // run animations:
                Animation anim = loadAnimation(f, f.getNextTransition(), true,
                        f.getNextTransitionStyle());
                if (anim != null) {
                    setHWLayerAnimListenerIfAlpha(f.mView, anim);
                    f.mView.startAnimation(anim);
                }
            }
        }
        if (f.mHiddenChanged) {
            completeShowHideFragment(f);
        }
    }

    /**
     * Changes the state of the fragment manager to {@code newState}. If the fragment manager
     * changes state or {@code always} is {@code true}, any fragments within it have their
     * states updated as well.
     *
     * @param newState The new state for the fragment manager
     * @param always If {@code true}, all fragments update their state, even
     *               if {@code newState} matches the current fragment manager's state.
     */
    void moveToState(int newState, boolean always) {
        if (mHost == null && newState != Fragment.INITIALIZING) {
            throw new IllegalStateException("No activity");
        }

        if (!always && newState == mCurState) {
            return;
        }

        mCurState = newState;

        if (mActive != null) {
            boolean loadersRunning = false;

            // Must add them in the proper order. mActive fragments may be out of order
            if (mAdded != null) {
                final int numAdded = mAdded.size();
                for (int i = 0; i < numAdded; i++) {
                    Fragment f = mAdded.get(i);
                    moveFragmentToExpectedState(f);
                    if (f.mLoaderManager != null) {
                        loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                    }
                }
            }

            // Now iterate through all active fragments. These will include those that are removed
            // and detached.
            final int numActive = mActive.size();
            for (int i = 0; i < numActive; i++) {
                Fragment f = mActive.get(i);
                if (f != null && (f.mRemoving || f.mDetached) && !f.mIsNewlyAdded) {
                    moveFragmentToExpectedState(f);
                    if (f.mLoaderManager != null) {
                        loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                    }
                }
            }

            if (!loadersRunning) {
                startPendingDeferredFragments();
            }

            if (mNeedMenuInvalidate && mHost != null && mCurState == Fragment.RESUMED) {
                mHost.onSupportInvalidateOptionsMenu();
                mNeedMenuInvalidate = false;
            }
        }
    }

    void startPendingDeferredFragments() {
        if (mActive == null) return;

        for (int i=0; i<mActive.size(); i++) {
            Fragment f = mActive.get(i);
            if (f != null) {
                performPendingDeferredStart(f);
            }
        }
    }

    void makeActive(Fragment f) {
        if (f.mIndex >= 0) {
            return;
        }

        if (mAvailIndices == null || mAvailIndices.size() <= 0) {
            if (mActive == null) {
                mActive = new ArrayList<Fragment>();
            }
            f.setIndex(mActive.size(), mParent);
            mActive.add(f);

        } else {
            f.setIndex(mAvailIndices.remove(mAvailIndices.size()-1), mParent);
            mActive.set(f.mIndex, f);
        }
        if (DEBUG) Log.v(TAG, "Allocated fragment index " + f);
    }

    void makeInactive(Fragment f) {
        if (f.mIndex < 0) {
            return;
        }

        if (DEBUG) Log.v(TAG, "Freeing fragment index " + f);
        mActive.set(f.mIndex, null);
        if (mAvailIndices == null) {
            mAvailIndices = new ArrayList<Integer>();
        }
        mAvailIndices.add(f.mIndex);
        mHost.inactivateFragment(f.mWho);
        f.initState();
    }

    public void addFragment(Fragment fragment, boolean moveToStateNow) {
        if (mAdded == null) {
            mAdded = new ArrayList<Fragment>();
        }
        if (DEBUG) Log.v(TAG, "add: " + fragment);
        makeActive(fragment);
        if (!fragment.mDetached) {
            if (mAdded.contains(fragment)) {
                throw new IllegalStateException("Fragment already added: " + fragment);
            }
            mAdded.add(fragment);
            fragment.mAdded = true;
            fragment.mRemoving = false;
            if (fragment.mView == null) {
                fragment.mHiddenChanged = false;
            }
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            if (moveToStateNow) {
                moveToState(fragment);
            }
        }
    }

    public void removeFragment(Fragment fragment) {
        if (DEBUG) Log.v(TAG, "remove: " + fragment + " nesting=" + fragment.mBackStackNesting);
        final boolean inactive = !fragment.isInBackStack();
        if (!fragment.mDetached || inactive) {
            if (mAdded != null) {
                mAdded.remove(fragment);
            }
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            fragment.mAdded = false;
            fragment.mRemoving = true;
        }
    }

    /**
     * Marks a fragment as hidden to be later animated in with
     * {@link #completeShowHideFragment(Fragment)}.
     *
     * @param fragment The fragment to be shown.
     */
    public void hideFragment(Fragment fragment) {
        if (DEBUG) Log.v(TAG, "hide: " + fragment);
        if (!fragment.mHidden) {
            fragment.mHidden = true;
            // Toggle hidden changed so that if a fragment goes through show/hide/show
            // it doesn't go through the animation.
            fragment.mHiddenChanged = !fragment.mHiddenChanged;
        }
    }

    /**
     * Marks a fragment as shown to be later animated in with
     * {@link #completeShowHideFragment(Fragment)}.
     *
     * @param fragment The fragment to be shown.
     */
    public void showFragment(Fragment fragment) {
        if (DEBUG) Log.v(TAG, "show: " + fragment);
        if (fragment.mHidden) {
            fragment.mHidden = false;
            // Toggle hidden changed so that if a fragment goes through show/hide/show
            // it doesn't go through the animation.
            fragment.mHiddenChanged = !fragment.mHiddenChanged;
        }
    }

    public void detachFragment(Fragment fragment) {
        if (DEBUG) Log.v(TAG, "detach: " + fragment);
        if (!fragment.mDetached) {
            fragment.mDetached = true;
            if (fragment.mAdded) {
                // We are not already in back stack, so need to remove the fragment.
                if (mAdded != null) {
                    if (DEBUG) Log.v(TAG, "remove from detach: " + fragment);
                    mAdded.remove(fragment);
                }
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    mNeedMenuInvalidate = true;
                }
                fragment.mAdded = false;
            }
        }
    }

    public void attachFragment(Fragment fragment) {
        if (DEBUG) Log.v(TAG, "attach: " + fragment);
        if (fragment.mDetached) {
            fragment.mDetached = false;
            if (!fragment.mAdded) {
                if (mAdded == null) {
                    mAdded = new ArrayList<Fragment>();
                }
                if (mAdded.contains(fragment)) {
                    throw new IllegalStateException("Fragment already added: " + fragment);
                }
                if (DEBUG) Log.v(TAG, "add from attach: " + fragment);
                mAdded.add(fragment);
                fragment.mAdded = true;
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    mNeedMenuInvalidate = true;
                }
            }
        }
    }

    @Override
    public Fragment findFragmentById(int id) {
        if (mAdded != null) {
            // First look through added fragments.
            for (int i=mAdded.size()-1; i>=0; i--) {
                Fragment f = mAdded.get(i);
                if (f != null && f.mFragmentId == id) {
                    return f;
                }
            }
        }
        if (mActive != null) {
            // Now for any known fragment.
            for (int i=mActive.size()-1; i>=0; i--) {
                Fragment f = mActive.get(i);
                if (f != null && f.mFragmentId == id) {
                    return f;
                }
            }
        }
        return null;
    }

    @Override
    public Fragment findFragmentByTag(String tag) {
        if (mAdded != null && tag != null) {
            // First look through added fragments.
            for (int i=mAdded.size()-1; i>=0; i--) {
                Fragment f = mAdded.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        if (mActive != null && tag != null) {
            // Now for any known fragment.
            for (int i=mActive.size()-1; i>=0; i--) {
                Fragment f = mActive.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        return null;
    }

    public Fragment findFragmentByWho(String who) {
        if (mActive != null && who != null) {
            for (int i=mActive.size()-1; i>=0; i--) {
                Fragment f = mActive.get(i);
                if (f != null && (f=f.findFragmentByWho(who)) != null) {
                    return f;
                }
            }
        }
        return null;
    }

    private void checkStateLoss() {
        if (mStateSaved) {
            throw new IllegalStateException(
                    "Can not perform this action after onSaveInstanceState");
        }
        if (mNoTransactionsBecause != null) {
            throw new IllegalStateException(
                    "Can not perform this action inside of " + mNoTransactionsBecause);
        }
    }

    /**
     * Adds an action to the queue of pending actions.
     *
     * @param action the action to add
     * @param allowStateLoss whether to allow loss of state information
     * @throws IllegalStateException if the activity has been destroyed
     */
    public void enqueueAction(OpGenerator action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        synchronized (this) {
            if (mDestroyed || mHost == null) {
                throw new IllegalStateException("Activity has been destroyed");
            }
            if (mPendingActions == null) {
                mPendingActions = new ArrayList<>();
            }
            mPendingActions.add(action);
            scheduleCommit();
        }
    }

    /**
     * Schedules the execution when one hasn't been scheduled already. This should happen
     * the first time {@link #enqueueAction(OpGenerator, boolean)} is called or when
     * a postponed transaction has been started with
     * {@link Fragment#startPostponedEnterTransition()}
     */
    private void scheduleCommit() {
        synchronized (this) {
            boolean postponeReady =
                    mPostponedTransactions != null && !mPostponedTransactions.isEmpty();
            boolean pendingReady = mPendingActions != null && mPendingActions.size() == 1;
            if (postponeReady || pendingReady) {
                mHost.getHandler().removeCallbacks(mExecCommit);
                mHost.getHandler().post(mExecCommit);
            }
        }
    }

    public int allocBackStackIndex(BackStackRecord bse) {
        synchronized (this) {
            if (mAvailBackStackIndices == null || mAvailBackStackIndices.size() <= 0) {
                if (mBackStackIndices == null) {
                    mBackStackIndices = new ArrayList<BackStackRecord>();
                }
                int index = mBackStackIndices.size();
                if (DEBUG) Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                mBackStackIndices.add(bse);
                return index;

            } else {
                int index = mAvailBackStackIndices.remove(mAvailBackStackIndices.size()-1);
                if (DEBUG) Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                mBackStackIndices.set(index, bse);
                return index;
            }
        }
    }

    public void setBackStackIndex(int index, BackStackRecord bse) {
        synchronized (this) {
            if (mBackStackIndices == null) {
                mBackStackIndices = new ArrayList<BackStackRecord>();
            }
            int N = mBackStackIndices.size();
            if (index < N) {
                if (DEBUG) Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                mBackStackIndices.set(index, bse);
            } else {
                while (N < index) {
                    mBackStackIndices.add(null);
                    if (mAvailBackStackIndices == null) {
                        mAvailBackStackIndices = new ArrayList<Integer>();
                    }
                    if (DEBUG) Log.v(TAG, "Adding available back stack index " + N);
                    mAvailBackStackIndices.add(N);
                    N++;
                }
                if (DEBUG) Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                mBackStackIndices.add(bse);
            }
        }
    }

    public void freeBackStackIndex(int index) {
        synchronized (this) {
            mBackStackIndices.set(index, null);
            if (mAvailBackStackIndices == null) {
                mAvailBackStackIndices = new ArrayList<Integer>();
            }
            if (DEBUG) Log.v(TAG, "Freeing back stack index " + index);
            mAvailBackStackIndices.add(index);
        }
    }

    /**
     * Broken out from exec*, this prepares for gathering and executing operations.
     *
     * @param allowStateLoss true if state loss should be ignored or false if it should be
     *                       checked.
     */
    private void ensureExecReady(boolean allowStateLoss) {
        if (mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        }

        if (Looper.myLooper() != mHost.getHandler().getLooper()) {
            throw new IllegalStateException("Must be called from main thread of fragment host");
        }

        if (!allowStateLoss) {
            checkStateLoss();
        }

        if (mTmpRecords == null) {
            mTmpRecords = new ArrayList<>();
            mTmpIsPop = new ArrayList<>();
        }
        mExecutingActions = true;
        try {
            executePostponedTransaction(null, null);
        } finally {
            mExecutingActions = false;
        }
    }

    public void execSingleAction(OpGenerator action, boolean allowStateLoss) {
        ensureExecReady(allowStateLoss);
        if (action.generateOps(mTmpRecords, mTmpIsPop)) {
            mExecutingActions = true;
            try {
                optimizeAndExecuteOps(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }

        doPendingDeferredStart();
    }

    /**
     * Broken out of exec*, this cleans up the mExecutingActions and the temporary structures
     * used in executing operations.
     */
    private void cleanupExec() {
        mExecutingActions = false;
        mTmpIsPop.clear();
        mTmpRecords.clear();
    }

    /**
     * Only call from main thread!
     */
    public boolean execPendingActions() {
        ensureExecReady(true);

        boolean didSomething = false;
        while (generateOpsForPendingActions(mTmpRecords, mTmpIsPop)) {
            mExecutingActions = true;
            try {
                optimizeAndExecuteOps(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
            didSomething = true;
        }

        doPendingDeferredStart();

        return didSomething;
    }

    /**
     * Complete the execution of transactions that have previously been postponed, but are
     * now ready.
     */
    private void executePostponedTransaction(ArrayList<BackStackRecord> records,
            ArrayList<Boolean> isRecordPop) {
        int numPostponed = mPostponedTransactions == null ? 0 : mPostponedTransactions.size();
        for (int i = 0; i < numPostponed; i++) {
            StartEnterTransitionListener listener = mPostponedTransactions.get(i);
            if (records != null && !listener.mIsBack) {
                int index = records.indexOf(listener.mRecord);
                if (index != -1 && isRecordPop.get(index)) {
                    listener.cancelTransaction();
                    continue;
                }
            }
            if (listener.isReady() || (records != null
                    && listener.mRecord.interactsWith(records, 0, records.size()))) {
                mPostponedTransactions.remove(i);
                i--;
                numPostponed--;
                int index;
                if (records != null && !listener.mIsBack
                        && (index = records.indexOf(listener.mRecord)) != -1
                        && isRecordPop.get(index)) {
                    // This is popping a postponed transaction
                    listener.cancelTransaction();
                } else {
                    listener.completeTransaction();
                }
            }
        }
    }

    /**
     * Optimizes BackStackRecord operations. This method merges operations of proximate records
     * that allow optimization. See {@link FragmentTransaction#setAllowOptimization(boolean)}.
     * <p>
     * For example, a transaction that adds to the back stack and then another that pops that
     * back stack record will be optimized.
     * <p>
     * Likewise, two transactions committed that are executed at the same time will be optimized
     * as well as two pop operations executed together.
     *
     * @param records The records pending execution
     * @param isRecordPop The direction that these records are being run.
     */
    private void optimizeAndExecuteOps(ArrayList<BackStackRecord> records,
            ArrayList<Boolean> isRecordPop) {
        if (records == null || records.isEmpty()) {
            return;
        }

        if (isRecordPop == null || records.size() != isRecordPop.size()) {
            throw new IllegalStateException("Internal error with the back stack records");
        }

        // Force start of any postponed transactions that interact with scheduled transactions:
        executePostponedTransaction(records, isRecordPop);

        final int numRecords = records.size();
        int startIndex = 0;
        for (int recordNum = 0; recordNum < numRecords; recordNum++) {
            final boolean canOptimize = records.get(recordNum).mAllowOptimization;
            if (!canOptimize) {
                // execute all previous transactions
                if (startIndex != recordNum) {
                    executeOpsTogether(records, isRecordPop, startIndex, recordNum);
                }
                // execute all unoptimized pop operations together or one add operation
                int optimizeEnd = recordNum + 1;
                if (isRecordPop.get(recordNum)) {
                    while (optimizeEnd < numRecords
                            && isRecordPop.get(optimizeEnd)
                            && !records.get(optimizeEnd).mAllowOptimization) {
                        optimizeEnd++;
                    }
                }
                executeOpsTogether(records, isRecordPop, recordNum, optimizeEnd);
                startIndex = optimizeEnd;
                recordNum = optimizeEnd - 1;
            }
        }
        if (startIndex != numRecords) {
            executeOpsTogether(records, isRecordPop, startIndex, numRecords);
        }
    }

    /**
     * Optimizes a subset of a list of BackStackRecords, all of which either allow optimization or
     * do not allow optimization.
     * @param records A list of BackStackRecords that are to be optimized
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex The index of the first record in <code>records</code> to be optimized
     * @param endIndex One more than the final record index in <code>records</code> to optimize.
     */
    private void executeOpsTogether(ArrayList<BackStackRecord> records,
            ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        final boolean allowOptimization = records.get(startIndex).mAllowOptimization;
        boolean addToBackStack = false;
        if (mTmpAddedFragments == null) {
            mTmpAddedFragments = new ArrayList<>();
        } else {
            mTmpAddedFragments.clear();
        }
        if (mAdded != null) {
            mTmpAddedFragments.addAll(mAdded);
        }
        for (int recordNum = startIndex; recordNum < endIndex; recordNum++) {
            final BackStackRecord record = records.get(recordNum);
            final boolean isPop = isRecordPop.get(recordNum);
            if (!isPop) {
                record.expandReplaceOps(mTmpAddedFragments);
            } else {
                record.trackAddedFragmentsInPop(mTmpAddedFragments);
            }
            addToBackStack = addToBackStack || record.mAddToBackStack;
        }
        mTmpAddedFragments.clear();

        if (!allowOptimization) {
            FragmentTransition.startTransitions(this, records, isRecordPop, startIndex, endIndex,
                    false);
        }
        executeOps(records, isRecordPop, startIndex, endIndex);

        int postponeIndex = endIndex;
        if (allowOptimization) {
            ArraySet<Fragment> addedFragments = new ArraySet<>();
            addAddedFragments(addedFragments);
            postponeIndex = postponePostponableTransactions(records, isRecordPop,
                    startIndex, endIndex, addedFragments);
            makeRemovedFragmentsInvisible(addedFragments);
        }

        if (postponeIndex != startIndex && allowOptimization) {
            // need to run something now
            FragmentTransition.startTransitions(this, records, isRecordPop, startIndex,
                    postponeIndex, true);
            moveToState(mCurState, true);
        }

        for (int recordNum = startIndex; recordNum < endIndex; recordNum++) {
            final BackStackRecord record = records.get(recordNum);
            final boolean isPop = isRecordPop.get(recordNum);
            if (isPop && record.mIndex >= 0) {
                freeBackStackIndex(record.mIndex);
                record.mIndex = -1;
            }
        }
        if (addToBackStack) {
            reportBackStackChanged();
        }
    }

    /**
     * Any fragments that were removed because they have been postponed should have their views
     * made invisible by setting their alpha to 0 on API >= 11 or setting visibility to INVISIBLE
     * on API < 11.
     *
     * @param fragments The fragments that were added during operation execution. Only the ones
     *                  that are no longer added will have their alpha changed.
     */
    private void makeRemovedFragmentsInvisible(ArraySet<Fragment> fragments) {
        final int numAdded = fragments.size();
        for (int i = 0; i < numAdded; i++) {
            final Fragment fragment = fragments.valueAt(i);
            if (!fragment.mAdded) {
                final View view = fragment.getView();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    fragment.getView().setVisibility(View.INVISIBLE);
                } else {
                    fragment.mPostponedAlpha = view.getAlpha();
                    view.setAlpha(0f);
                }
            }
        }
    }

    /**
     * Examine all transactions and determine which ones are marked as postponed. Those will
     * have their operations rolled back and moved to the end of the record list (up to endIndex).
     * It will also add the postponed transaction to the queue.
     *
     * @param records A list of BackStackRecords that should be checked.
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex The index of the first record in <code>records</code> to be checked
     * @param endIndex One more than the final record index in <code>records</code> to be checked.
     * @return The index of the first postponed transaction or endIndex if no transaction was
     * postponed.
     */
    private int postponePostponableTransactions(ArrayList<BackStackRecord> records,
            ArrayList<Boolean> isRecordPop, int startIndex, int endIndex,
            ArraySet<Fragment> added) {
        int postponeIndex = endIndex;
        for (int i = endIndex - 1; i >= startIndex; i--) {
            final BackStackRecord record = records.get(i);
            final boolean isPop = isRecordPop.get(i);
            boolean isPostponed = record.isPostponed()
                    && !record.interactsWith(records, i + 1, endIndex);
            if (isPostponed) {
                if (mPostponedTransactions == null) {
                    mPostponedTransactions = new ArrayList<>();
                }
                StartEnterTransitionListener listener =
                        new StartEnterTransitionListener(record, isPop);
                mPostponedTransactions.add(listener);
                record.setOnStartPostponedListener(listener);

                // roll back the transaction
                if (isPop) {
                    record.executeOps();
                } else {
                    record.executePopOps(false);
                }

                // move to the end
                postponeIndex--;
                if (i != postponeIndex) {
                    records.remove(i);
                    records.add(postponeIndex, record);
                }

                // different views may be visible now
                addAddedFragments(added);
            }
        }
        return postponeIndex;
    }

    /**
     * When a postponed transaction is ready to be started, this completes the transaction,
     * removing, hiding, or showing views as well as starting the animations and transitions.
     * <p>
     * {@code runtransitions} is set to false when the transaction postponement was interrupted
     * abnormally -- normally by a new transaction being started that affects the postponed
     * transaction.
     *
     * @param record The transaction to run
     * @param isPop true if record is popping or false if it is adding
     * @param runTransitions true if the fragment transition should be run or false otherwise.
     * @param moveToState true if the state should be changed after executing the operations.
     *                    This is false when the transaction is canceled when a postponed
     *                    transaction is popped.
     */
    private void completeExecute(BackStackRecord record, boolean isPop, boolean runTransitions,
            boolean moveToState) {
        ArrayList<BackStackRecord> records = new ArrayList<>(1);
        ArrayList<Boolean> isRecordPop = new ArrayList<>(1);
        records.add(record);
        isRecordPop.add(isPop);
        executeOps(records, isRecordPop, 0, 1);
        if (runTransitions) {
            FragmentTransition.startTransitions(this, records, isRecordPop, 0, 1, true);
        }
        if (moveToState) {
            moveToState(mCurState, true);
        }

        if (mActive != null) {
            final int numActive = mActive.size();
            for (int i = 0; i < numActive; i++) {
                // Allow added fragments to be removed during the pop since we aren't going
                // to move them to the final state with moveToState(mCurState).
                Fragment fragment = mActive.get(i);
                if (fragment != null && fragment.mView != null && fragment.mIsNewlyAdded
                        && record.interactsWith(fragment.mContainerId)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                            && fragment.mPostponedAlpha > 0) {
                        fragment.mView.setAlpha(fragment.mPostponedAlpha);
                    }
                    if (moveToState) {
                        fragment.mPostponedAlpha = 0;
                    } else {
                        fragment.mPostponedAlpha = -1;
                        fragment.mIsNewlyAdded = false;
                    }
                }
            }
        }
    }

    /**
     * Find a fragment within the fragment's container whose View should be below the passed
     * fragment. {@code null} is returned when the fragment has no View or if there should be
     * no fragment with a View below the given fragment.
     *
     * As an example, if mAdded has two Fragments with Views sharing the same container:
     * FragmentA
     * FragmentB
     *
     * Then, when processing FragmentB, FragmentA will be returned. If, however, FragmentA
     * had no View, null would be returned.
     *
     * @param f The fragment that may be on top of another fragment.
     * @return The fragment with a View under f, if one exists or null if f has no View or
     * there are no fragments with Views in the same container.
     */
    private Fragment findFragmentUnder(Fragment f) {
        final ViewGroup container = f.mContainer;
        final View view = f.mView;

        if (container == null || view == null) {
            return null;
        }

        final int fragmentIndex = mAdded.indexOf(f);
        for (int i = fragmentIndex - 1; i >= 0; i--) {
            Fragment underFragment = mAdded.get(i);
            if (underFragment.mContainer == container && underFragment.mView != null) {
                // Found the fragment under this one
                return underFragment;
            }
        }
        return null;
    }

    /**
     * Run the operations in the BackStackRecords, either to push or pop.
     *
     * @param records The list of records whose operations should be run.
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex The index of the first entry in records to run.
     * @param endIndex One past the index of the final entry in records to run.
     */
    private static void executeOps(ArrayList<BackStackRecord> records,
            ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            final BackStackRecord record = records.get(i);
            final boolean isPop = isRecordPop.get(i);
            if (isPop) {
                record.bumpBackStackNesting(-1);
                // Only execute the add operations at the end of
                // all transactions.
                boolean moveToState = i == (endIndex - 1);
                record.executePopOps(moveToState);
            } else {
                record.bumpBackStackNesting(1);
                record.executeOps();
            }
        }
    }

    /**
     * Ensure that fragments that are added are moved to at least the CREATED state.
     * Any newly-added Views are inserted into {@code added} so that the Transaction can be
     * postponed with {@link Fragment#postponeEnterTransition()}. They will later be made
     * invisible (by setting their alpha to 0) if they have been removed when postponed.
     */
    private void addAddedFragments(ArraySet<Fragment> added) {
        if (mCurState < Fragment.CREATED) {
            return;
        }
        // We want to leave the fragment in the started state
        final int state = Math.min(mCurState, Fragment.STARTED);
        final int numAdded = mAdded == null ? 0 : mAdded.size();
        for (int i = 0; i < numAdded; i++) {
            Fragment fragment = mAdded.get(i);
            if (fragment.mState < state) {
                moveToState(fragment, state, fragment.getNextAnim(), fragment.getNextTransition(),
                        false);
                if (fragment.mView != null && !fragment.mHidden && fragment.mIsNewlyAdded) {
                    added.add(fragment);
                }
            }
        }
    }

    /**
     * Starts all postponed transactions regardless of whether they are ready or not.
     */
    private void forcePostponedTransactions() {
        if (mPostponedTransactions != null) {
            while (!mPostponedTransactions.isEmpty()) {
                mPostponedTransactions.remove(0).completeTransaction();
            }
        }
    }

    /**
     * Ends the animations of fragments so that they immediately reach the end state.
     * This is used prior to saving the state so that the correct state is saved.
     */
    private void endAnimatingAwayFragments() {
        final int numFragments = mActive == null ? 0 : mActive.size();
        for (int i = 0; i < numFragments; i++) {
            Fragment fragment = mActive.get(i);
            if (fragment != null && fragment.getAnimatingAway() != null) {
                // Give up waiting for the animation and just end it.
                final int stateAfterAnimating = fragment.getStateAfterAnimating();
                final View animatingAway = fragment.getAnimatingAway();
                fragment.setAnimatingAway(null);
                Animation animation = animatingAway.getAnimation();
                if (animation != null) {
                    animation.cancel();
                }
                moveToState(fragment, stateAfterAnimating, 0, 0, false);
            }
        }
    }

    /**
     * Adds all records in the pending actions to records and whether they are add or pop
     * operations to isPop. After executing, the pending actions will be empty.
     *
     * @param records All pending actions will generate BackStackRecords added to this.
     *                This contains the transactions, in order, to execute.
     * @param isPop All pending actions will generate booleans to add to this. This contains
     *              an entry for each entry in records to indicate whether or not it is a
     *              pop action.
     */
    private boolean generateOpsForPendingActions(ArrayList<BackStackRecord> records,
            ArrayList<Boolean> isPop) {
        int numActions;
        synchronized (this) {
            if (mPendingActions == null || mPendingActions.size() == 0) {
                return false;
            }

            numActions = mPendingActions.size();
            for (int i = 0; i < numActions; i++) {
                mPendingActions.get(i).generateOps(records, isPop);
            }
            mPendingActions.clear();
            mHost.getHandler().removeCallbacks(mExecCommit);
        }
        return numActions > 0;
    }

    void doPendingDeferredStart() {
        if (mHavePendingDeferredStart) {
            boolean loadersRunning = false;
            for (int i = 0; i < mActive.size(); i++) {
                Fragment f = mActive.get(i);
                if (f != null && f.mLoaderManager != null) {
                    loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                }
            }
            if (!loadersRunning) {
                mHavePendingDeferredStart = false;
                startPendingDeferredFragments();
            }
        }
    }

    void reportBackStackChanged() {
        if (mBackStackChangeListeners != null) {
            for (int i=0; i<mBackStackChangeListeners.size(); i++) {
                mBackStackChangeListeners.get(i).onBackStackChanged();
            }
        }
    }

    void addBackStackState(BackStackRecord state) {
        if (mBackStack == null) {
            mBackStack = new ArrayList<BackStackRecord>();
        }
        mBackStack.add(state);
        reportBackStackChanged();
    }

    @SuppressWarnings("unused")
    boolean popBackStackState(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop,
            String name, int id, int flags) {
        if (mBackStack == null) {
            return false;
        }
        if (name == null && id < 0 && (flags & POP_BACK_STACK_INCLUSIVE) == 0) {
            int last = mBackStack.size() - 1;
            if (last < 0) {
                return false;
            }
            records.add(mBackStack.remove(last));
            isRecordPop.add(true);
        } else {
            int index = -1;
            if (name != null || id >= 0) {
                // If a name or ID is specified, look for that place in
                // the stack.
                index = mBackStack.size()-1;
                while (index >= 0) {
                    BackStackRecord bss = mBackStack.get(index);
                    if (name != null && name.equals(bss.getName())) {
                        break;
                    }
                    if (id >= 0 && id == bss.mIndex) {
                        break;
                    }
                    index--;
                }
                if (index < 0) {
                    return false;
                }
                if ((flags&POP_BACK_STACK_INCLUSIVE) != 0) {
                    index--;
                    // Consume all following entries that match.
                    while (index >= 0) {
                        BackStackRecord bss = mBackStack.get(index);
                        if ((name != null && name.equals(bss.getName()))
                                || (id >= 0 && id == bss.mIndex)) {
                            index--;
                            continue;
                        }
                        break;
                    }
                }
            }
            if (index == mBackStack.size()-1) {
                return false;
            }
            for (int i = mBackStack.size() - 1; i > index; i--) {
                records.add(mBackStack.remove(i));
                isRecordPop.add(true);
            }
        }
        return true;
    }

    FragmentManagerNonConfig retainNonConfig() {
        ArrayList<Fragment> fragments = null;
        ArrayList<FragmentManagerNonConfig> childFragments = null;
        if (mActive != null) {
            for (int i=0; i<mActive.size(); i++) {
                Fragment f = mActive.get(i);
                if (f != null) {
                    if (f.mRetainInstance) {
                        if (fragments == null) {
                            fragments = new ArrayList<Fragment>();
                        }
                        fragments.add(f);
                        f.mRetaining = true;
                        f.mTargetIndex = f.mTarget != null ? f.mTarget.mIndex : -1;
                        if (DEBUG) Log.v(TAG, "retainNonConfig: keeping retained " + f);
                    }
                    boolean addedChild = false;
                    if (f.mChildFragmentManager != null) {
                        FragmentManagerNonConfig child = f.mChildFragmentManager.retainNonConfig();
                        if (child != null) {
                            if (childFragments == null) {
                                childFragments = new ArrayList<FragmentManagerNonConfig>();
                                for (int j = 0; j < i; j++) {
                                    childFragments.add(null);
                                }
                            }
                            childFragments.add(child);
                            addedChild = true;
                        }
                    }
                    if (childFragments != null && !addedChild) {
                        childFragments.add(null);
                    }
                }
            }
        }
        if (fragments == null && childFragments == null) {
            return null;
        }
        return new FragmentManagerNonConfig(fragments, childFragments);
    }

    void saveFragmentViewState(Fragment f) {
        if (f.mInnerView == null) {
            return;
        }
        if (mStateArray == null) {
            mStateArray = new SparseArray<Parcelable>();
        } else {
            mStateArray.clear();
        }
        f.mInnerView.saveHierarchyState(mStateArray);
        if (mStateArray.size() > 0) {
            f.mSavedViewState = mStateArray;
            mStateArray = null;
        }
    }

    Bundle saveFragmentBasicState(Fragment f) {
        Bundle result = null;

        if (mStateBundle == null) {
            mStateBundle = new Bundle();
        }
        f.performSaveInstanceState(mStateBundle);
        dispatchOnFragmentSaveInstanceState(f, mStateBundle, false);
        if (!mStateBundle.isEmpty()) {
            result = mStateBundle;
            mStateBundle = null;
        }

        if (f.mView != null) {
            saveFragmentViewState(f);
        }
        if (f.mSavedViewState != null) {
            if (result == null) {
                result = new Bundle();
            }
            result.putSparseParcelableArray(
                    FragmentManagerImpl.VIEW_STATE_TAG, f.mSavedViewState);
        }
        if (!f.mUserVisibleHint) {
            if (result == null) {
                result = new Bundle();
            }
            // Only add this if it's not the default value
            result.putBoolean(FragmentManagerImpl.USER_VISIBLE_HINT_TAG, f.mUserVisibleHint);
        }

        return result;
    }

    Parcelable saveAllState() {
        // Make sure all pending operations have now been executed to get
        // our state update-to-date.
        forcePostponedTransactions();
        endAnimatingAwayFragments();
        execPendingActions();

        if (HONEYCOMB) {
            // As of Honeycomb, we save state after pausing.  Prior to that
            // it is before pausing.  With fragments this is an issue, since
            // there are many things you may do after pausing but before
            // stopping that change the fragment state.  For those older
            // devices, we will not at this point say that we have saved
            // the state, so we will allow them to continue doing fragment
            // transactions.  This retains the same semantics as Honeycomb,
            // though you do have the risk of losing the very most recent state
            // if the process is killed...  we'll live with that.
            mStateSaved = true;
        }

        if (mActive == null || mActive.size() <= 0) {
            return null;
        }

        // First collect all active fragments.
        int N = mActive.size();
        FragmentState[] active = new FragmentState[N];
        boolean haveFragments = false;
        for (int i=0; i<N; i++) {
            Fragment f = mActive.get(i);
            if (f != null) {
                if (f.mIndex < 0) {
                    throwException(new IllegalStateException(
                            "Failure saving state: active " + f
                            + " has cleared index: " + f.mIndex));
                }

                haveFragments = true;

                FragmentState fs = new FragmentState(f);
                active[i] = fs;

                if (f.mState > Fragment.INITIALIZING && fs.mSavedFragmentState == null) {
                    fs.mSavedFragmentState = saveFragmentBasicState(f);

                    if (f.mTarget != null) {
                        if (f.mTarget.mIndex < 0) {
                            throwException(new IllegalStateException(
                                    "Failure saving state: " + f
                                    + " has target not in fragment manager: " + f.mTarget));
                        }
                        if (fs.mSavedFragmentState == null) {
                            fs.mSavedFragmentState = new Bundle();
                        }
                        putFragment(fs.mSavedFragmentState,
                                FragmentManagerImpl.TARGET_STATE_TAG, f.mTarget);
                        if (f.mTargetRequestCode != 0) {
                            fs.mSavedFragmentState.putInt(
                                    FragmentManagerImpl.TARGET_REQUEST_CODE_STATE_TAG,
                                    f.mTargetRequestCode);
                        }
                    }

                } else {
                    fs.mSavedFragmentState = f.mSavedFragmentState;
                }

                if (DEBUG) Log.v(TAG, "Saved state of " + f + ": "
                        + fs.mSavedFragmentState);
            }
        }

        if (!haveFragments) {
            if (DEBUG) Log.v(TAG, "saveAllState: no fragments!");
            return null;
        }

        int[] added = null;
        BackStackState[] backStack = null;

        // Build list of currently added fragments.
        if (mAdded != null) {
            N = mAdded.size();
            if (N > 0) {
                added = new int[N];
                for (int i=0; i<N; i++) {
                    added[i] = mAdded.get(i).mIndex;
                    if (added[i] < 0) {
                        throwException(new IllegalStateException(
                                "Failure saving state: active " + mAdded.get(i)
                                + " has cleared index: " + added[i]));
                    }
                    if (DEBUG) Log.v(TAG, "saveAllState: adding fragment #" + i
                            + ": " + mAdded.get(i));
                }
            }
        }

        // Now save back stack.
        if (mBackStack != null) {
            N = mBackStack.size();
            if (N > 0) {
                backStack = new BackStackState[N];
                for (int i=0; i<N; i++) {
                    backStack[i] = new BackStackState(mBackStack.get(i));
                    if (DEBUG) Log.v(TAG, "saveAllState: adding back stack #" + i
                            + ": " + mBackStack.get(i));
                }
            }
        }

        FragmentManagerState fms = new FragmentManagerState();
        fms.mActive = active;
        fms.mAdded = added;
        fms.mBackStack = backStack;
        return fms;
    }

    void restoreAllState(Parcelable state, FragmentManagerNonConfig nonConfig) {
        // If there is no saved state at all, then there can not be
        // any nonConfig fragments either, so that is that.
        if (state == null) return;
        FragmentManagerState fms = (FragmentManagerState)state;
        if (fms.mActive == null) return;

        List<FragmentManagerNonConfig> childNonConfigs = null;

        // First re-attach any non-config instances we are retaining back
        // to their saved state, so we don't try to instantiate them again.
        if (nonConfig != null) {
            List<Fragment> nonConfigFragments = nonConfig.getFragments();
            childNonConfigs = nonConfig.getChildNonConfigs();
            final int count = nonConfigFragments != null ? nonConfigFragments.size() : 0;
            for (int i = 0; i < count; i++) {
                Fragment f = nonConfigFragments.get(i);
                if (DEBUG) Log.v(TAG, "restoreAllState: re-attaching retained " + f);
                FragmentState fs = fms.mActive[f.mIndex];
                fs.mInstance = f;
                f.mSavedViewState = null;
                f.mBackStackNesting = 0;
                f.mInLayout = false;
                f.mAdded = false;
                f.mTarget = null;
                if (fs.mSavedFragmentState != null) {
                    fs.mSavedFragmentState.setClassLoader(mHost.getContext().getClassLoader());
                    f.mSavedViewState = fs.mSavedFragmentState.getSparseParcelableArray(
                            FragmentManagerImpl.VIEW_STATE_TAG);
                    f.mSavedFragmentState = fs.mSavedFragmentState;
                }
            }
        }

        // Build the full list of active fragments, instantiating them from
        // their saved state.
        mActive = new ArrayList<>(fms.mActive.length);
        if (mAvailIndices != null) {
            mAvailIndices.clear();
        }
        for (int i=0; i<fms.mActive.length; i++) {
            FragmentState fs = fms.mActive[i];
            if (fs != null) {
                FragmentManagerNonConfig childNonConfig = null;
                if (childNonConfigs != null && i < childNonConfigs.size()) {
                    childNonConfig = childNonConfigs.get(i);
                }
                Fragment f = fs.instantiate(mHost, mParent, childNonConfig);
                if (DEBUG) Log.v(TAG, "restoreAllState: active #" + i + ": " + f);
                mActive.add(f);
                // Now that the fragment is instantiated (or came from being
                // retained above), clear mInstance in case we end up re-restoring
                // from this FragmentState again.
                fs.mInstance = null;
            } else {
                mActive.add(null);
                if (mAvailIndices == null) {
                    mAvailIndices = new ArrayList<Integer>();
                }
                if (DEBUG) Log.v(TAG, "restoreAllState: avail #" + i);
                mAvailIndices.add(i);
            }
        }

        // Update the target of all retained fragments.
        if (nonConfig != null) {
            List<Fragment> nonConfigFragments = nonConfig.getFragments();
            final int count = nonConfigFragments != null ? nonConfigFragments.size() : 0;
            for (int i = 0; i < count; i++) {
                Fragment f = nonConfigFragments.get(i);
                if (f.mTargetIndex >= 0) {
                    if (f.mTargetIndex < mActive.size()) {
                        f.mTarget = mActive.get(f.mTargetIndex);
                    } else {
                        Log.w(TAG, "Re-attaching retained fragment " + f
                                + " target no longer exists: " + f.mTargetIndex);
                        f.mTarget = null;
                    }
                }
            }
        }

        // Build the list of currently added fragments.
        if (fms.mAdded != null) {
            mAdded = new ArrayList<Fragment>(fms.mAdded.length);
            for (int i=0; i<fms.mAdded.length; i++) {
                Fragment f = mActive.get(fms.mAdded[i]);
                if (f == null) {
                    throwException(new IllegalStateException(
                            "No instantiated fragment for index #" + fms.mAdded[i]));
                }
                f.mAdded = true;
                if (DEBUG) Log.v(TAG, "restoreAllState: added #" + i + ": " + f);
                if (mAdded.contains(f)) {
                    throw new IllegalStateException("Already added!");
                }
                mAdded.add(f);
            }
        } else {
            mAdded = null;
        }

        // Build the back stack.
        if (fms.mBackStack != null) {
            mBackStack = new ArrayList<BackStackRecord>(fms.mBackStack.length);
            for (int i=0; i<fms.mBackStack.length; i++) {
                BackStackRecord bse = fms.mBackStack[i].instantiate(this);
                if (DEBUG) {
                    Log.v(TAG, "restoreAllState: back stack #" + i
                        + " (index " + bse.mIndex + "): " + bse);
                    LogWriter logw = new LogWriter(TAG);
                    PrintWriter pw = new PrintWriter(logw);
                    bse.dump("  ", pw, false);
                    pw.close();
                }
                mBackStack.add(bse);
                if (bse.mIndex >= 0) {
                    setBackStackIndex(bse.mIndex, bse);
                }
            }
        } else {
            mBackStack = null;
        }
    }

    public void attachController(FragmentHostCallback host,
            FragmentContainer container, Fragment parent) {
        if (mHost != null) throw new IllegalStateException("Already attached");
        mHost = host;
        mContainer = container;
        mParent = parent;
    }

    public void noteStateNotSaved() {
        mStateSaved = false;
    }

    public void dispatchCreate() {
        mStateSaved = false;
        mExecutingActions = true;
        moveToState(Fragment.CREATED, false);
        mExecutingActions = false;
    }

    public void dispatchActivityCreated() {
        mStateSaved = false;
        mExecutingActions = true;
        moveToState(Fragment.ACTIVITY_CREATED, false);
        mExecutingActions = false;
    }

    public void dispatchStart() {
        mStateSaved = false;
        mExecutingActions = true;
        moveToState(Fragment.STARTED, false);
        mExecutingActions = false;
    }

    public void dispatchResume() {
        mStateSaved = false;
        mExecutingActions = true;
        moveToState(Fragment.RESUMED, false);
        mExecutingActions = false;
    }

    public void dispatchPause() {
        mExecutingActions = true;
        moveToState(Fragment.STARTED, false);
        mExecutingActions = false;
    }

    public void dispatchStop() {
        // See saveAllState() for the explanation of this.  We do this for
        // all platform versions, to keep our behavior more consistent between
        // them.
        mStateSaved = true;

        mExecutingActions = true;
        moveToState(Fragment.STOPPED, false);
        mExecutingActions = false;
    }

    public void dispatchReallyStop() {
        mExecutingActions = true;
        moveToState(Fragment.ACTIVITY_CREATED, false);
        mExecutingActions = false;
    }

    public void dispatchDestroyView() {
        mExecutingActions = true;
        moveToState(Fragment.CREATED, false);
        mExecutingActions = false;
    }

    public void dispatchDestroy() {
        mDestroyed = true;
        execPendingActions();
        mExecutingActions = true;
        moveToState(Fragment.INITIALIZING, false);
        mExecutingActions = false;
        mHost = null;
        mContainer = null;
        mParent = null;
    }

    public void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode) {
        if (mAdded == null) {
            return;
        }
        for (int i = mAdded.size() - 1; i >= 0; --i) {
            final android.support.v4.app.Fragment f = mAdded.get(i);
            if (f != null) {
                f.performMultiWindowModeChanged(isInMultiWindowMode);
            }
        }
    }

    public void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (mAdded == null) {
            return;
        }
        for (int i = mAdded.size() - 1; i >= 0; --i) {
            final android.support.v4.app.Fragment f = mAdded.get(i);
            if (f != null) {
                f.performPictureInPictureModeChanged(isInPictureInPictureMode);
            }
        }
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        if (mAdded != null) {
            for (int i=0; i<mAdded.size(); i++) {
                Fragment f = mAdded.get(i);
                if (f != null) {
                    f.performConfigurationChanged(newConfig);
                }
            }
        }
    }

    public void dispatchLowMemory() {
        if (mAdded != null) {
            for (int i=0; i<mAdded.size(); i++) {
                Fragment f = mAdded.get(i);
                if (f != null) {
                    f.performLowMemory();
                }
            }
        }
    }

    public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean show = false;
        ArrayList<Fragment> newMenus = null;
        if (mAdded != null) {
            for (int i=0; i<mAdded.size(); i++) {
                Fragment f = mAdded.get(i);
                if (f != null) {
                    if (f.performCreateOptionsMenu(menu, inflater)) {
                        show = true;
                        if (newMenus == null) {
                            newMenus = new ArrayList<Fragment>();
                        }
                        newMenus.add(f);
                    }
                }
            }
        }

        if (mCreatedMenus != null) {
            for (int i=0; i<mCreatedMenus.size(); i++) {
                Fragment f = mCreatedMenus.get(i);
                if (newMenus == null || !newMenus.contains(f)) {
                    f.onDestroyOptionsMenu();
                }
            }
        }

        mCreatedMenus = newMenus;

        return show;
    }

    public boolean dispatchPrepareOptionsMenu(Menu menu) {
        boolean show = false;
        if (mAdded != null) {
            for (int i=0; i<mAdded.size(); i++) {
                Fragment f = mAdded.get(i);
                if (f != null) {
                    if (f.performPrepareOptionsMenu(menu)) {
                        show = true;
                    }
                }
            }
        }
        return show;
    }

    public boolean dispatchOptionsItemSelected(MenuItem item) {
        if (mAdded != null) {
            for (int i=0; i<mAdded.size(); i++) {
                Fragment f = mAdded.get(i);
                if (f != null) {
                    if (f.performOptionsItemSelected(item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean dispatchContextItemSelected(MenuItem item) {
        if (mAdded != null) {
            for (int i=0; i<mAdded.size(); i++) {
                Fragment f = mAdded.get(i);
                if (f != null) {
                    if (f.performContextItemSelected(item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void dispatchOptionsMenuClosed(Menu menu) {
        if (mAdded != null) {
            for (int i=0; i<mAdded.size(); i++) {
                Fragment f = mAdded.get(i);
                if (f != null) {
                    f.performOptionsMenuClosed(menu);
                }
            }
        }
    }

    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb,
            boolean recursive) {
        if (mLifecycleCallbacks == null) {
            mLifecycleCallbacks = new CopyOnWriteArrayList<>();
        }
        mLifecycleCallbacks.add(new Pair(cb, recursive));
    }

    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb) {
        if (mLifecycleCallbacks == null) {
            return;
        }

        synchronized (mLifecycleCallbacks) {
            for (int i = 0, N = mLifecycleCallbacks.size(); i < N; i++) {
                if (mLifecycleCallbacks.get(i).first == cb) {
                    mLifecycleCallbacks.remove(i);
                    break;
                }
            }
        }
    }

    void dispatchOnFragmentPreAttached(Fragment f, Context context, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentPreAttached(f, context, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentPreAttached(this, f, context);
            }
        }
    }

    void dispatchOnFragmentAttached(Fragment f, Context context, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentAttached(f, context, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentAttached(this, f, context);
            }
        }
    }

    void dispatchOnFragmentCreated(Fragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentCreated(f, savedInstanceState, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentCreated(this, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentActivityCreated(Fragment f, Bundle savedInstanceState,
            boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentActivityCreated(f, savedInstanceState, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentActivityCreated(this, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentViewCreated(Fragment f, View v, Bundle savedInstanceState,
            boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentViewCreated(f, v, savedInstanceState, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentViewCreated(this, f, v, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentStarted(Fragment f, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentStarted(f, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentStarted(this, f);
            }
        }
    }

    void dispatchOnFragmentResumed(Fragment f, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentResumed(f, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentResumed(this, f);
            }
        }
    }

    void dispatchOnFragmentPaused(Fragment f, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentPaused(f, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentPaused(this, f);
            }
        }
    }

    void dispatchOnFragmentStopped(Fragment f, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentStopped(f, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentStopped(this, f);
            }
        }
    }

    void dispatchOnFragmentSaveInstanceState(Fragment f, Bundle outState, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentSaveInstanceState(f, outState, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentSaveInstanceState(this, f, outState);
            }
        }
    }

    void dispatchOnFragmentViewDestroyed(Fragment f, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentViewDestroyed(f, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentViewDestroyed(this, f);
            }
        }
    }

    void dispatchOnFragmentDestroyed(Fragment f, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentDestroyed(f, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentDestroyed(this, f);
            }
        }
    }

    void dispatchOnFragmentDetached(Fragment f, boolean onlyRecursive) {
        if (mParent != null) {
            FragmentManager parentManager = mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager)
                        .dispatchOnFragmentDetached(f, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentDetached(this, f);
            }
        }
    }

    public static int reverseTransit(int transit) {
        int rev = 0;
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_FADE;
                break;
        }
        return rev;

    }

    public static final int ANIM_STYLE_OPEN_ENTER = 1;
    public static final int ANIM_STYLE_OPEN_EXIT = 2;
    public static final int ANIM_STYLE_CLOSE_ENTER = 3;
    public static final int ANIM_STYLE_CLOSE_EXIT = 4;
    public static final int ANIM_STYLE_FADE_ENTER = 5;
    public static final int ANIM_STYLE_FADE_EXIT = 6;

    public static int transitToStyleIndex(int transit, boolean enter) {
        int animAttr = -1;
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                animAttr = enter ? ANIM_STYLE_OPEN_ENTER : ANIM_STYLE_OPEN_EXIT;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                animAttr = enter ? ANIM_STYLE_CLOSE_ENTER : ANIM_STYLE_CLOSE_EXIT;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                animAttr = enter ? ANIM_STYLE_FADE_ENTER : ANIM_STYLE_FADE_EXIT;
                break;
        }
        return animAttr;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (!"fragment".equals(name)) {
            return null;
        }

        String fname = attrs.getAttributeValue(null, "class");
        TypedArray a =  context.obtainStyledAttributes(attrs, FragmentTag.Fragment);
        if (fname == null) {
            fname = a.getString(FragmentTag.Fragment_name);
        }
        int id = a.getResourceId(FragmentTag.Fragment_id, View.NO_ID);
        String tag = a.getString(FragmentTag.Fragment_tag);
        a.recycle();

        if (!Fragment.isSupportFragmentClass(mHost.getContext(), fname)) {
            // Invalid support lib fragment; let the device's framework handle it.
            // This will allow android.app.Fragments to do the right thing.
            return null;
        }

        int containerId = parent != null ? parent.getId() : 0;
        if (containerId == View.NO_ID && id == View.NO_ID && tag == null) {
            throw new IllegalArgumentException(attrs.getPositionDescription()
                    + ": Must specify unique android:id, android:tag, or have a parent with an id for " + fname);
        }

        // If we restored from a previous state, we may already have
        // instantiated this fragment from the state and should use
        // that instance instead of making a new one.
        Fragment fragment = id != View.NO_ID ? findFragmentById(id) : null;
        if (fragment == null && tag != null) {
            fragment = findFragmentByTag(tag);
        }
        if (fragment == null && containerId != View.NO_ID) {
            fragment = findFragmentById(containerId);
        }

        if (FragmentManagerImpl.DEBUG) Log.v(TAG, "onCreateView: id=0x"
                + Integer.toHexString(id) + " fname=" + fname
                + " existing=" + fragment);
        if (fragment == null) {
            fragment = Fragment.instantiate(context, fname);
            fragment.mFromLayout = true;
            fragment.mFragmentId = id != 0 ? id : containerId;
            fragment.mContainerId = containerId;
            fragment.mTag = tag;
            fragment.mInLayout = true;
            fragment.mFragmentManager = this;
            fragment.mHost = mHost;
            fragment.onInflate(mHost.getContext(), attrs, fragment.mSavedFragmentState);
            addFragment(fragment, true);

        } else if (fragment.mInLayout) {
            // A fragment already exists and it is not one we restored from
            // previous state.
            throw new IllegalArgumentException(attrs.getPositionDescription()
                    + ": Duplicate id 0x" + Integer.toHexString(id)
                    + ", tag " + tag + ", or parent id 0x" + Integer.toHexString(containerId)
                    + " with another fragment for " + fname);
        } else {
            // This fragment was retained from a previous instance; get it
            // going now.
            fragment.mInLayout = true;
            fragment.mHost = mHost;
            // If this fragment is newly instantiated (either right now, or
            // from last saved state), then give it the attributes to
            // initialize itself.
            if (!fragment.mRetaining) {
                fragment.onInflate(mHost.getContext(), attrs, fragment.mSavedFragmentState);
            }
        }

        // If we haven't finished entering the CREATED state ourselves yet,
        // push the inflated child fragment along.
        if (mCurState < Fragment.CREATED && fragment.mFromLayout) {
            moveToState(fragment, Fragment.CREATED, 0, 0, false);
        } else {
            moveToState(fragment);
        }

        if (fragment.mView == null) {
            throw new IllegalStateException("Fragment " + fname
                    + " did not create a view.");
        }
        if (id != 0) {
            fragment.mView.setId(id);
        }
        if (fragment.mView.getTag() == null) {
            fragment.mView.setTag(tag);
        }
        return fragment.mView;
    }

    LayoutInflaterFactory getLayoutInflaterFactory() {
        return this;
    }

    static class FragmentTag {
        public static final int[] Fragment = {
                0x01010003, 0x010100d0, 0x010100d1
        };
        public static final int Fragment_id = 1;
        public static final int Fragment_name = 0;
        public static final int Fragment_tag = 2;
    }

    /**
     * An add or pop transaction to be scheduled for the UI thread.
     */
    interface OpGenerator {
        /**
         * Generate transactions to add to {@code records} and whether or not the transaction is
         * an add or pop to {@code isRecordPop}.
         *
         * records and isRecordPop must be added equally so that each transaction in records
         * matches the boolean for whether or not it is a pop in isRecordPop.
         *
         * @param records A list to add transactions to.
         * @param isRecordPop A list to add whether or not the transactions added to records is
         *                    a pop transaction.
         * @return true if something was added or false otherwise.
         */
        boolean generateOps(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop);
    }

    /**
     * A pop operation OpGenerator. This will be run on the UI thread and will generate the
     * transactions that will be popped if anything can be popped.
     */
    private class PopBackStackState implements OpGenerator {
        final String mName;
        final int mId;
        final int mFlags;

        PopBackStackState(String name, int id, int flags) {
            mName = name;
            mId = id;
            mFlags = flags;
        }

        @Override
        public boolean generateOps(ArrayList<BackStackRecord> records,
                ArrayList<Boolean> isRecordPop) {
            return popBackStackState(records, isRecordPop, mName, mId, mFlags);
        }
    }

    /**
     * A listener for a postponed transaction. This waits until
     * {@link Fragment#startPostponedEnterTransition()} is called or a transaction is started
     * that interacts with this one, based on interactions with the fragment container.
     */
    static class StartEnterTransitionListener
            implements Fragment.OnStartEnterTransitionListener {
        private final boolean mIsBack;
        private final BackStackRecord mRecord;
        private int mNumPostponed;

        StartEnterTransitionListener(BackStackRecord record, boolean isBack) {
            mIsBack = isBack;
            mRecord = record;
        }

        /**
         * Called from {@link Fragment#startPostponedEnterTransition()}, this decreases the
         * number of Fragments that are postponed. This may cause the transaction to schedule
         * to finish running and run transitions and animations.
         */
        @Override
        public void onStartEnterTransition() {
            mNumPostponed--;
            if (mNumPostponed != 0) {
                return;
            }
            mRecord.mManager.scheduleCommit();
        }

        /**
         * Called from {@link Fragment#
         * setOnStartEnterTransitionListener(Fragment.OnStartEnterTransitionListener)}, this
         * increases the number of fragments that are postponed as part of this transaction.
         */
        @Override
        public void startListening() {
            mNumPostponed++;
        }

        /**
         * @return true if there are no more postponed fragments as part of the transaction.
         */
        public boolean isReady() {
            return mNumPostponed == 0;
        }

        /**
         * Completes the transaction and start the animations and transitions. This may skip
         * the transitions if this is called before all fragments have called
         * {@link Fragment#startPostponedEnterTransition()}.
         */
        public void completeTransaction() {
            final boolean canceled;
            canceled = mNumPostponed > 0;
            FragmentManagerImpl manager = mRecord.mManager;
            final int numAdded = manager.mAdded.size();
            for (int i = 0; i < numAdded; i++) {
                final Fragment fragment = manager.mAdded.get(i);
                fragment.setOnStartEnterTransitionListener(null);
                if (canceled && fragment.isPostponed()) {
                    fragment.startPostponedEnterTransition();
                }
            }
            mRecord.mManager.completeExecute(mRecord, mIsBack, !canceled, true);
        }

        /**
         * Cancels this transaction instead of completing it. That means that the state isn't
         * changed, so the pop results in no change to the state.
         */
        public void cancelTransaction() {
            mRecord.mManager.completeExecute(mRecord, mIsBack, false, false);
        }
    }
}
