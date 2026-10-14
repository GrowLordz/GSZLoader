package com.gsz.loader;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IShizukuShell extends IInterface {
    String exec(String command) throws RemoteException;

    abstract class Stub extends Binder implements IShizukuShell {
        private static final String DESCRIPTOR = "com.gsz.loader.IShizukuShell";
        static final int TRANSACTION_exec = IBinder.FIRST_CALL_TRANSACTION + 0;

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IShizukuShell asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IShizukuShell) return (IShizukuShell) iin;
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            if (code == TRANSACTION_exec) {
                data.enforceInterface(DESCRIPTOR);
                String _arg0 = data.readString();
                String _result = this.exec(_arg0);
                reply.writeNoException();
                reply.writeString(_result);
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IShizukuShell {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            public IBinder asBinder() {
                return mRemote;
            }

            public String exec(String command) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                String result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(command);
                    mRemote.transact(TRANSACTION_exec, data, reply, 0);
                    reply.readException();
                    result = reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }
        }
    }
}
