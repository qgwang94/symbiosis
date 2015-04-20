/* =========================================================================
 * File: $Id: Cache.java,v 1.1 2010/06/18 17:01:13 smhuang Exp $Cache.java,v$
 *
 * Copyright (c) 2006, Yuriy Stepovoy. All rights reserved.
 * email: stepovoy@gmail.com
 *
 * =========================================================================
 */

package net.sf.cache4j;

/**
 * Cache ��������� ������� � �������� ����
 *
 * @version $Revision: 1.1 $ $Date: 2010/06/18 17:01:13 $
 * @author Yuriy Stepovoy. <a href="mailto:stepovoy@gmail.com">stepovoy@gmail.com</a>
 **/

public interface Cache {
// ----------------------------------------------------------------------------- ���������
// ----------------------------------------------------------------------------- �������� ������
// ----------------------------------------------------------------------------- ����������� ����������
// ----------------------------------------------------------------------------- ������������
// ----------------------------------------------------------------------------- Public ������

    /**
     * �������� ������ � ���.
     * @param objId ������������� �������
     * @param obj ������
     * @throws CacheException ���� �������� ��������
     */
    public void put(Object objId, Object obj) throws CacheException;

    /**
     * ���������� ������ �� ����.
     * @param objId ������������� �������
     * @return ������ ������������ ������ � ��� ������, ���� ������ ������
     * � ����� ����� ������� �� ����������� � �� ��������� ����� �����������.
     * @throws CacheException ���� �������� ��������
     */
    public Object get(Object objId) throws CacheException;

    /**
     * ������� ������ �� ����
     * @param objId ������������� �������
     * @throws CacheException ���� �������� ��������
     */
    public void remove(Object objId) throws CacheException;

    /**
     * ���������� ���������� �������� � ����
     */
    public int size();

    /**
     * ������� ��� ������� �� ����
     * @throws CacheException ���� �������� ��������
     */
    public void clear() throws CacheException;

    /**
     * ���������� ����������� ����
     */
    public CacheConfig getCacheConfig();

    /**
     * ���������� ���������� � ����
     */
    public CacheInfo getCacheInfo();

// ----------------------------------------------------------------------------- Package scope ������
// ----------------------------------------------------------------------------- Protected ������
// ----------------------------------------------------------------------------- Private ������
// ----------------------------------------------------------------------------- Inner ������
}

/*
$Log: Cache.java,v $
Revision 1.1  2010/06/18 17:01:13  smhuang
*** empty log message ***

*/
