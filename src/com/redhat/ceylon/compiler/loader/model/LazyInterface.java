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

package com.redhat.ceylon.compiler.loader.model;

import java.util.List;

import com.redhat.ceylon.compiler.java.util.Util;
import com.redhat.ceylon.compiler.loader.AbstractModelLoader;
import com.redhat.ceylon.compiler.loader.ModelCompleter;
import com.redhat.ceylon.compiler.loader.mirror.ClassMirror;
import com.redhat.ceylon.compiler.typechecker.model.Annotation;
import com.redhat.ceylon.compiler.typechecker.model.ClassOrInterface;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Interface;
import com.redhat.ceylon.compiler.typechecker.model.ProducedReference;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.Scope;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypeParameter;
import com.redhat.ceylon.compiler.typechecker.model.Unit;

/**
 * Represents a lazy Interface declaration.
 *
 * @author Stéphane Épardaud <stef@epardaud.fr>
 */
public class LazyInterface extends Interface implements LazyContainer {

    public ClassMirror classMirror;
    private ModelCompleter completer;
    private String realName;
    private boolean isStatic;
    private boolean isCeylon;
    
    private boolean isLoaded = false;
    private boolean isTypeParamsLoaded = false;

    public LazyInterface(ClassMirror classMirror, ModelCompleter completer) {
        this.classMirror = classMirror;
        this.completer = completer;
        this.realName = classMirror.getSimpleName();
        setName(Util.strip(this.realName));
        this.isStatic = classMirror.isStatic();
        this.isCeylon = classMirror.getAnnotation(AbstractModelLoader.CEYLON_CEYLON_ANNOTATION) != null;
    }

    public boolean isCeylon() {
        return isCeylon;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getRealName() {
        return this.realName;
    }
    
    private void load() {
        loadTypeParams();
        if(!isLoaded){
            isLoaded = true;
            completer.complete(this);
        }
    }

    private void loadTypeParams() {
        if(!isTypeParamsLoaded){
            isTypeParamsLoaded = true;
            completer.completeTypeParameters(this);
        }
    }
    
    @Override
    public String toString() {
        if (!isLoaded) {
            return "UNLOADED:" + super.toString();
        }
        return super.toString();
    }

    @Override
    public List<Declaration> getMembers() {
        load();
        return super.getMembers();
    }

    @Override
    public ProducedType getType() {
        loadTypeParams();
        return super.getType();
    }

    @Override
    public ProducedType getExtendedType() {
        load();
        return super.getExtendedType();
    }

    @Override
    public List<TypeParameter> getTypeParameters() {
        loadTypeParams();
        return super.getTypeParameters();
    }

    @Override
    public boolean isMember() {
        return super.isMember();
    }

    @Override
    public ProducedType getDeclaringType(Declaration d) {
        load();
        return super.getDeclaringType(d);
    }

    @Override
    public boolean isParameterized() {
        load();
        return super.isParameterized();
    }

    @Override
    public ClassOrInterface getExtendedTypeDeclaration() {
        load();
        return super.getExtendedTypeDeclaration();
    }

    @Override
    public List<TypeDeclaration> getSatisfiedTypeDeclarations() {
        load();
        return super.getSatisfiedTypeDeclarations();
    }

    @Override
    public List<ProducedType> getSatisfiedTypes() {
        load();
        return super.getSatisfiedTypes();
    }

    @Override
    public List<TypeDeclaration> getCaseTypeDeclarations() {
        load();
        return super.getCaseTypeDeclarations();
    }

    @Override
    public List<ProducedType> getCaseTypes() {
        load();
        return super.getCaseTypes();
    }

    @Override
    public ProducedReference getProducedReference(ProducedType pt,
            List<ProducedType> typeArguments) {
        loadTypeParams();
        return super.getProducedReference(pt, typeArguments);
    }

    @Override
    public ProducedType getProducedType(ProducedType outerType,
            List<ProducedType> typeArguments) {
        loadTypeParams();
        return super.getProducedType(outerType, typeArguments);
    }

    @Override
    public List<Declaration> getInheritedMembers(String name) {
        load();
        return super.getInheritedMembers(name);
    }

    @Override
    public Declaration getRefinedMember(String name, List<ProducedType> signature) {
        load();
        return super.getRefinedMember(name, signature);
    }

    @Override
    public Declaration getMember(String name, List<ProducedType> signature) {
        load();
        return super.getMember(name, signature);
    }

    @Override
    public Declaration getMemberOrParameter(String name, List<ProducedType> signature) {
        load();
        return super.getMemberOrParameter(name, signature);
    }

    @Override
    public boolean isAlias() {
        load();
        return super.isAlias();
    }

    @Override
    public ProducedType getSelfType() {
        load();
        return super.getSelfType();
    }

    @Override
    public Scope getVisibleScope() {
        load();
        return super.getVisibleScope();
    }

    @Override
    public List<Annotation> getAnnotations() {
        load();
        return super.getAnnotations();
    }

    @Override
    public String getQualifiedNameString() {
        load();
        return super.getQualifiedNameString();
    }

    @Override
    public boolean isActual() {
        load();
        return super.isActual();
    }

    @Override
    public boolean isFormal() {
        load();
        return super.isFormal();
    }

    @Override
    public boolean isDefault() {
        load();
        return super.isDefault();
    }

    @Override
    public boolean isVisible(Scope scope) {
        load();
        return super.isVisible(scope);
    }

    @Override
    public boolean isDefinedInScope(Scope scope) {
        load();
        return super.isDefinedInScope(scope);
    }

    @Override
    public boolean isCaptured() {
        load();
        return super.isCaptured();
    }

    @Override
    public boolean isToplevel() {
        load();
        return super.isToplevel();
    }

    @Override
    public boolean isClassMember() {
        load();
        return super.isClassMember();
    }

    @Override
    public boolean isInterfaceMember() {
        load();
        return super.isInterfaceMember();
    }

    @Override
    public boolean isClassOrInterfaceMember() {
        load();
        return super.isClassOrInterfaceMember();
    }

    @Override
    public Unit getUnit() {
        // this doesn't require to load the model
        return super.getUnit();
    }

    @Override
    public Scope getContainer() {
        return super.getContainer();
    }

    @Override
    public Declaration getDirectMemberOrParameter(String name, List<ProducedType> signature) {
        load();
        return super.getDirectMemberOrParameter(name, signature);
    }

    @Override
    public Declaration getDirectMember(String name, List<ProducedType> signature) {
        load();
        return super.getDirectMember(name, signature);
    }

    @Override
    public Declaration getMemberOrParameter(Unit unit, String name, List<ProducedType> signature) {
        load();
        return super.getMemberOrParameter(unit, name, signature);
    }

    @Override
    public void addMember(Declaration decl) {
        // do this without lazy-loading
        super.getMembers().add(decl);
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }
}
