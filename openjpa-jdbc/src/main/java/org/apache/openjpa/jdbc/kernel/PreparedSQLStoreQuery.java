/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.jdbc.kernel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.ResultSetResult;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.lib.rop.RangeResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * A executor for Prepared SQL Query.
 * 
 * @author Pinaki Poddar
 *
 */
public class PreparedSQLStoreQuery extends SQLStoreQuery {
    public PreparedSQLStoreQuery(JDBCStore store) {
        super(store);
    }
    
    public Executor newDataStoreExecutor(ClassMetaData meta,
        boolean subclasses) {
        return new PreparedSQLExecutor(this, meta);
    }
    
    public static class PreparedSQLExecutor extends AbstractExecutor {
        private final ClassMetaData _meta;
        public PreparedSQLExecutor(PreparedSQLStoreQuery q, 
            ClassMetaData candidate) {
            _meta = candidate;
        }
               
        public int getOperation(StoreQuery q) {
            return OP_SELECT;
        }

        public ResultObjectProvider executeQuery(StoreQuery q,
            Object[] params, Range range) {
            JDBCStore store = ((SQLStoreQuery) q).getStore();
            DBDictionary dict = store.getDBDictionary();

            SQLBuffer buf = new SQLBuffer(dict)
                .append(q.getContext().getQueryString());
            Connection conn = store.getConnection();
            JDBCFetchConfiguration fetch = (JDBCFetchConfiguration)
                q.getContext().getFetchConfiguration();

            ResultObjectProvider rop;
            PreparedStatement stmnt = null;
            try {
                stmnt = !range.lrs
                    ? buf.prepareStatement(conn)
                    : buf.prepareStatement(conn, fetch, -1, -1);

                int index = 0;
                for (int i = 0; i < params.length; i++)
                    dict.setUnknown(stmnt, ++index, params[i], null);

                ResultSet rs = stmnt.executeQuery();
                ResultSetResult res = new ResultSetResult(conn, stmnt, rs, 
                    store);
                if (q.getContext().getCandidateType() != null)
                    rop = new GenericResultObjectProvider((ClassMapping) _meta,
                        store, fetch, res);
                else
                    rop = new SQLProjectionResultObjectProvider(store, fetch,
                        res, q.getContext().getResultType());
            } catch (SQLException se) {
                if (stmnt != null)
                    try { stmnt.close(); } catch (SQLException se2) {}
                try { conn.close(); } catch (SQLException se2) {}
                throw SQLExceptions.getStore(se, dict);
            }

            if (range.start != 0 || range.end != Long.MAX_VALUE)
                rop = new RangeResultObjectProvider(rop, range.start,range.end);
            return rop;
        }
        
        /**
         * Convert given userParams to an array whose ordering matches as 
         * per expected during executeXXX() methods.
         * The given userParams is already re-parameterized, so this method have 
         * to merely copy the given Map values.
         * 
         * @see PreparedQueryImpl#reparametrize(Map, org.apache.openjpa.kernel.Broker)
         */
        public Object[] toParameterArray(StoreQuery q, Map userParams) {
            Object[] array = new Object[userParams.size()];
            for (Object key : userParams.keySet()) {
                int idx = ((Integer)key).intValue();
                array[idx] = userParams.get(key);
            }
            return array;
        }
    }
}
