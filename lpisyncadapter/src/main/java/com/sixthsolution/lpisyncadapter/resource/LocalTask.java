package com.sixthsolution.lpisyncadapter.resource;

/**
 * @author mehdok (mehdok@gmail.com) on 4/8/2017.
 */

public class LocalTask implements LocalResource {
    @Override
    public Long getId() {
        return null;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public String getETag() {
        return null;
    }

    @Override
    public int delete() {
        return 0;
    }

    @Override
    public void prepareForUpload() {

    }

    @Override
    public void clearDirty(String eTag) {

    }
}
