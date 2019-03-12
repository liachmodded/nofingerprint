/*
 * MIT License
 *
 * Copyright (c) 2019
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.liachmodded.nofingerprint;

import java.util.ListIterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("com.github.liachmodded.nofingerprint")
@IFMLLoadingPlugin.Name("No Fingerprint")
public final class NoFingerprint implements IFMLLoadingPlugin {

  static final Logger LOGGER = LogManager.getLogger("NoFingerprint");

  @Override
  public String[] getASMTransformerClass() {
    return new String[]{"com.github.liachmodded.nofingerprint.ModContainerTransformer"};
  }

  @Nullable
  @Override
  public String getModContainerClass() {
    return null;
  }

  @Nullable
  @Override
  public String getSetupClass() {
    return null;
  }

  @Override
  public void injectData(Map<String, Object> data) {
  }

  @Nullable
  @Override
  public String getAccessTransformerClass() {
    return null;
  }
}

@SuppressWarnings("unused")
final class ModContainerTransformer implements IClassTransformer {

  private static final String FML_MOD_CONTAINER_VM_NAME = "Lnet/minecraftforge/fml/common/FMLModContainer;";

  @Override
  public byte[] transform(String name, String transformedName, byte[] basicClass) {
    if (!"net.minecraftforge.fml.common.FMLModContainer".equals(transformedName)) {
      return basicClass;
    }
    ClassReader classReader = new ClassReader(basicClass);
    ClassNode classNode = new ClassNode();
    classReader.accept(classNode, 0); // Just check annotations

    MethodNode constructMod = null;
    for (MethodNode each : classNode.methods) {
      if (each.name.equals("constructMod")) {
        constructMod = each;
      }
    }

    if (constructMod == null) {
      NoFingerprint.LOGGER.error("Cannot find constructMod method!");
      return basicClass;
    }

    InsnList list = constructMod.instructions;
    for (ListIterator<AbstractInsnNode> iterator = list.iterator();
        iterator.hasNext(); ) {
      AbstractInsnNode current = iterator.next();
      if (current.getOpcode() == Opcodes.PUTFIELD) {
        FieldInsnNode fieldInsnNode = (FieldInsnNode) current;
        if (fieldInsnNode.owner.equals(FML_MOD_CONTAINER_VM_NAME) && fieldInsnNode.name
            .equals("fingerprintNotPresent") && fieldInsnNode.desc.equals("Z")) {
          list.insertBefore(fieldInsnNode, new InsnNode(Opcodes.POP));
          list.insert(fieldInsnNode, new InsnNode(Opcodes.ICONST_0));
        }
      }
    }

    ClassWriter classWriter = new ClassWriter(0);
    classNode.accept(classWriter);
    return classWriter.toByteArray();
  }
}
