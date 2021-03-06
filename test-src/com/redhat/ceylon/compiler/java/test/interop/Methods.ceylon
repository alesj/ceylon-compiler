/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
import java.io{File}

@nomodel
@error
void methods() {
    @error
    File f1 = File("file1");
    @error
    File f2 = File("file2");
    @error
    print(f1.canRead());
    Integer cmp = f1.compareTo(f2);
    @error
    f1.listFiles();
}

@error
@nomodel
void overloadedMethodsAndSubClasses() {
    @error
    JavaWithOverloadedMembersSubClass inst = JavaWithOverloadedMembersSubClass();
    @error
    inst.method();
    @error
    inst.method(1);
    @error
    inst.method(1, 2);
    @error
    inst.topMethod();
}

@error
@nomodel
void overloadedConstructors() {
    @error
    JavaWithOverloadedMembersSubClass inst = JavaWithOverloadedMembersSubClass();
    @error
    JavaWithOverloadedMembersSubClass inst2 = JavaWithOverloadedMembersSubClass(2);
}

@error
@nomodel
class OverloadedMembersAndSubClasses() extends JavaWithOverloadedMembersSubClass() {
    void test(){
        @error
        method();
        @error
        method(1);
        @error
        method(1, 2);
        @error
        topMethod();
    }
}

@error
@nomodel
class OverloadedMembersAndSubClasses2() extends JavaWithOverloadedMembersSubClass(2) {
}