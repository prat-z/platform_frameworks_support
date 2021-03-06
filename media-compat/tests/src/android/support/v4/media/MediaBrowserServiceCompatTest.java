/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.support.v4.media;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Test {@link android.support.v4.media.MediaBrowserServiceCompat}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaBrowserServiceCompatTest {
    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;
    private static final long WAIT_TIME_FOR_NO_RESPONSE_MS = 500L;
    private static final ComponentName TEST_BROWSER_SERVICE = new ComponentName(
            "android.support.mediacompat.test",
            "android.support.v4.media.StubMediaBrowserServiceCompat");
    private final Object mWaitLock = new Object();

    private final ConnectionCallback mConnectionCallback = new ConnectionCallback();
    private final SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback();
    private final ItemCallback mItemCallback = new ItemCallback();
    private final SearchCallback mSearchCallback = new SearchCallback();

    private MediaBrowserCompat mMediaBrowser;
    private StubMediaBrowserServiceCompat mMediaBrowserService;
    private Bundle mRootHints;

    @Before
    public void setUp() throws Exception {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mRootHints = new Bundle();
                mRootHints.putBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_RECENT, true);
                mRootHints.putBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_OFFLINE, true);
                mRootHints.putBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_SUGGESTED, true);
                mMediaBrowser = new MediaBrowserCompat(getInstrumentation().getTargetContext(),
                        TEST_BROWSER_SERVICE, mConnectionCallback, mRootHints);
            }
        });
        synchronized (mWaitLock) {
            mMediaBrowser.connect();
            mWaitLock.wait(TIME_OUT_MS);
        }
        assertNotNull(mMediaBrowserService);
    }

    @Test
    @SmallTest
    public void testGetSessionToken() {
        assertEquals(StubMediaBrowserServiceCompat.sSession.getSessionToken(),
                mMediaBrowserService.getSessionToken());
    }

    @Test
    @SmallTest
    public void testNotifyChildrenChanged() throws Exception {
        synchronized (mWaitLock) {
            mSubscriptionCallback.reset();
            mMediaBrowser.subscribe(
                    StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, mSubscriptionCallback);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSubscriptionCallback.mOnChildrenLoaded);

            mSubscriptionCallback.reset();
            mMediaBrowserService.notifyChildrenChanged(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSubscriptionCallback.mOnChildrenLoaded);
        }
    }

    @Test
    @SmallTest
    public void testNotifyChildrenChangedWithPagination() throws Exception {
        synchronized (mWaitLock) {
            final int pageSize = 5;
            final int page = 2;
            Bundle options = new Bundle();
            options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
            options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);

            mSubscriptionCallback.reset();
            mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT, options,
                    mSubscriptionCallback);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSubscriptionCallback.mOnChildrenLoadedWithOptions);

            mSubscriptionCallback.reset();
            mMediaBrowserService.notifyChildrenChanged(StubMediaBrowserServiceCompat.MEDIA_ID_ROOT);
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSubscriptionCallback.mOnChildrenLoadedWithOptions);
        }
    }

    @Test
    @LargeTest
    public void testDelayedNotifyChildrenChanged() throws Exception {
        synchronized (mWaitLock) {
            mSubscriptionCallback.reset();
            mMediaBrowser.subscribe(StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN_DELAYED,
                    mSubscriptionCallback);
            mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertFalse(mSubscriptionCallback.mOnChildrenLoaded);

            mMediaBrowserService.sendDelayedNotifyChildrenChanged();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSubscriptionCallback.mOnChildrenLoaded);

            mSubscriptionCallback.reset();
            mMediaBrowserService.notifyChildrenChanged(
                    StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN_DELAYED);
            mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertFalse(mSubscriptionCallback.mOnChildrenLoaded);

            mMediaBrowserService.sendDelayedNotifyChildrenChanged();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSubscriptionCallback.mOnChildrenLoaded);
        }
    }

    // TODO(hdmoon): Uncomment after fixing failing tests. (Fails on API >= 24)
    // @Test
    // @SmallTest
    public void testDelayedItem() throws Exception {
        synchronized (mWaitLock) {
            mItemCallback.reset();
            mMediaBrowser.getItem(
                    StubMediaBrowserServiceCompat.MEDIA_ID_CHILDREN_DELAYED, mItemCallback);
            mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertFalse(mItemCallback.mOnItemLoaded);

            mItemCallback.reset();
            mMediaBrowserService.sendDelayedItemLoaded();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mItemCallback.mOnItemLoaded);
        }
    }

    @Test
    @SmallTest
    public void testSearch() throws Exception {
        final String key = "test-key";
        final String val = "test-val";

        synchronized (mWaitLock) {
            mSearchCallback.reset();
            mMediaBrowser.search(StubMediaBrowserServiceCompat.SEARCH_QUERY_FOR_NO_RESULT, null,
                    mSearchCallback);
            mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertTrue(mSearchCallback.mOnSearchResult);
            assertTrue(mSearchCallback.mSearchResults != null
                    && mSearchCallback.mSearchResults.size() == 0);
            assertEquals(null, mSearchCallback.mSearchExtras);

            mSearchCallback.reset();
            mMediaBrowser.search(StubMediaBrowserServiceCompat.SEARCH_QUERY_FOR_ERROR, null,
                    mSearchCallback);
            mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertTrue(mSearchCallback.mOnSearchResult);
            assertNull(mSearchCallback.mSearchResults);
            assertEquals(null, mSearchCallback.mSearchExtras);

            mSearchCallback.reset();
            Bundle extras = new Bundle();
            extras.putString(key, val);
            mMediaBrowser.search(StubMediaBrowserServiceCompat.SEARCH_QUERY, extras,
                    mSearchCallback);
            mWaitLock.wait(WAIT_TIME_FOR_NO_RESPONSE_MS);
            assertTrue(mSearchCallback.mOnSearchResult);
            assertNotNull(mSearchCallback.mSearchResults);
            for (MediaItem item : mSearchCallback.mSearchResults) {
                assertNotNull(item.getMediaId());
                assertTrue(item.getMediaId().contains(StubMediaBrowserServiceCompat.SEARCH_QUERY));
            }
            assertNotNull(mSearchCallback.mSearchExtras);
            assertEquals(val, mSearchCallback.mSearchExtras.getString(key));
        }
    }

    @Test
    @SmallTest
    public void testBrowserRoot() {
        final String id = "test-id";
        final String key = "test-key";
        final String val = "test-val";
        final Bundle extras = new Bundle();
        extras.putString(key, val);

        MediaBrowserServiceCompat.BrowserRoot browserRoot =
                new MediaBrowserServiceCompat.BrowserRoot(id, extras);
        assertEquals(id, browserRoot.getRootId());
        assertEquals(val, browserRoot.getExtras().getString(key));
    }

    private void assertRootHints(MediaItem item) {
        Bundle rootHints = item.getDescription().getExtras();
        assertNotNull(rootHints);
        assertEquals(mRootHints.getBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_RECENT),
                rootHints.getBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_RECENT));
        assertEquals(mRootHints.getBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_OFFLINE),
                rootHints.getBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_OFFLINE));
        assertEquals(mRootHints.getBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_SUGGESTED),
                rootHints.getBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_SUGGESTED));
    }

    private class ConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        @Override
        public void onConnected() {
            synchronized (mWaitLock) {
                mMediaBrowserService = StubMediaBrowserServiceCompat.sInstance;
                mWaitLock.notify();
            }
        }
    }

    private class SubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {
        boolean mOnChildrenLoaded;
        boolean mOnChildrenLoadedWithOptions;

        @Override
        public void onChildrenLoaded(String parentId, List<MediaItem> children) {
            synchronized (mWaitLock) {
                mOnChildrenLoaded = true;
                if (children != null) {
                    for (MediaItem item : children) {
                        assertRootHints(item);
                    }
                }
                mWaitLock.notify();
            }
        }

        @Override
        public void onChildrenLoaded(String parentId, List<MediaItem> children, Bundle options) {
            synchronized (mWaitLock) {
                mOnChildrenLoadedWithOptions = true;
                if (children != null) {
                    for (MediaItem item : children) {
                        assertRootHints(item);
                    }
                }
                mWaitLock.notify();
            }
        }

        public void reset() {
            mOnChildrenLoaded = false;
            mOnChildrenLoadedWithOptions = false;
        }
    }

    private class ItemCallback extends MediaBrowserCompat.ItemCallback {
        boolean mOnItemLoaded;

        @Override
        public void onItemLoaded(MediaItem item) {
            synchronized (mWaitLock) {
                mOnItemLoaded = true;
                assertRootHints(item);
                mWaitLock.notify();
            }
        }

        public void reset() {
            mOnItemLoaded = false;
        }
    }

    private class SearchCallback extends MediaBrowserCompat.SearchCallback {
        boolean mOnSearchResult;
        Bundle mSearchExtras;
        List<MediaItem> mSearchResults;

        @Override
        public void onSearchResult(String query, Bundle extras, List<MediaItem> items) {
            synchronized (mWaitLock) {
                mOnSearchResult = true;
                mSearchResults = items;
                mSearchExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onError(String query, Bundle extras) {
            synchronized (mWaitLock) {
                mOnSearchResult = true;
                mSearchResults = null;
                mSearchExtras = extras;
                mWaitLock.notify();
            }
        }

        public void reset() {
            mOnSearchResult = false;
            mSearchExtras = null;
            mSearchResults = null;
        }
    }
}
