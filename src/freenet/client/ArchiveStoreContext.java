/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package freenet.client;

import freenet.keys.FreenetURI;
import freenet.support.DoublyLinkedListImpl;

/**
 * Tracks all files currently in the cache from a given key.
 * Keeps the last known hash of the key (if this changes in a fetch, we flush the cache, unpack,
 * then throw an ArchiveRestartedException).
 * Provides fetch methods for Fetcher, which try the cache and then fetch if necessary, 
 * subject to the above.
 * 
 * Always take the lock on ArchiveStoreContext before the lock on ArchiveManager, NOT the other way around.
 */
public class ArchiveStoreContext {

	private FreenetURI key;
	private short archiveType;
	/** Archive size */
	private long lastSize = -1;
	/** Archive hash */
	private byte[] lastHash;
	/** Index of still-cached ArchiveStoreItems with this key.
	 * Note that we never ever hold this and then take another lock! In particular
	 * we must not take the ArchiveManager lock while holding this lock. It must be
	 * the inner lock to avoid deadlocks. */
	private final DoublyLinkedListImpl myItems;
	
	ArchiveStoreContext(FreenetURI key, short archiveType) {
		this.key = key;
		this.archiveType = archiveType;
		myItems = new DoublyLinkedListImpl();
	}

	/** Returns the size of the archive last time we fetched it, or -1 */
	long getLastSize() {
		return lastSize;
	}
	
	/** Sets the size of the archive - @see getLastSize() */
	void setLastSize(long size) {
		lastSize = size;
	}

	
	/** Returns the hash of the archive last time we fetched it, or null */
	byte[] getLastHash() {
		return lastHash;
	}

	/** Sets the hash of the archive - @see getLastHash() */
	void setLastHash(byte[] realHash) {
		lastHash = realHash;
	}

	/**
	 * Remove all ArchiveStoreItems with this key from the cache.
	 */
	void removeAllCachedItems(ArchiveManager manager) {
		ArchiveStoreItem item = null;
		while(true) {
			synchronized (myItems) {
				item = (ArchiveStoreItem) myItems.pop();
			}
			if(item == null) break;
			manager.removeCachedItem(item);
		}
	}

	/** Notify that a new archive store item with this key has been added to the cache. */
	void addItem(ArchiveStoreItem item) {
		synchronized(myItems) {
			myItems.push(item);
		}
	}

	/** Notify that an archive store item with this key has been expelled from the 
	 * cache. Remove it from our local cache and ask it to free the bucket if 
	 * necessary. */
	void removeItem(ArchiveStoreItem item) {
		synchronized(myItems) {
			if(myItems.remove(item) == null) return; // only removed once
		}
		item.innerClose();
	}

	public short getArchiveType() {
		return archiveType;
	}
	
	public FreenetURI getKey() {
		return key;
	}

}
