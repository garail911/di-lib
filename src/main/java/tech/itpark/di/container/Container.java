package tech.itpark.di.container;

import tech.itpark.di.exception.AmbiguousConstructorException;
import tech.itpark.di.exception.DIException;
import tech.itpark.di.exception.UnmetDependencyException;

import java.lang.reflect.Constructor;
import java.util.*;

public class Container {
    private final Map<String, Object> objects = new HashMap<>();
    private final Collection<Definition> definitions = new LinkedList<>();


    public void register(Definition... definitions) { // definitions на самом деле массив
        this.definitions.addAll(Arrays.asList(definitions));
    }


    public void wire() {
        try {
            List<Definition> lost = new LinkedList<>();
            for (Definition definition : definitions) {
                if (definition.getDependencies().length != 0) {
                    lost.add(definition);
                    continue;
                }

                final Class<?> cls = Class.forName(definition.getName());
                final Object o = cls.getDeclaredConstructor().newInstance();
                objects.put(definition.getName(), o);
                for (Class<?> iface : cls.getInterfaces()) {
                    objects.put(iface.getName(), o);
                }
            }
            while (true) {
                final var iterator = lost.iterator();

                if (definitions.size() == 0) {
                    break;
                }

                while (iterator.hasNext()) {
                    final var definition = iterator.next();

                    if (!objects.keySet().containsAll(Arrays.asList(definition.getDependencies()))) {
                        continue;
                    }

                    final Class<?> cls = Class.forName(definition.getName());

                    final Constructor<?>[] constructors = cls.getConstructors();
                    if (constructors.length != 1) {
                        throw new AmbiguousConstructorException("component must have only one public constructor");
                    }

                    final Constructor<?> constructor = constructors[0];
                    final Class<?>[] parameterTypes = constructor.getParameterTypes();
                    final List<Object> parameters = new LinkedList<>();
                    // iter
                    for (Class<?> parameterType : parameterTypes) {
                        // objects.get(Repository.class.getName)
                        final Object parameter = objects.get(parameterType.getName());
                        if (parameter == null) {
                            throw new UnmetDependencyException(
                                    "unmet dependency for "
                                            + cls.getName()
                                            + " not found parameter with type "
                                            + parameterType.getName()
                            );
                        }
                        parameters.add(parameter);
                    }
                    final Object o = constructor.newInstance(parameters.toArray());
                    objects.put(o.getClass().getName(), o);
                    iterator.remove(); // удаляем из списка несозданных
                }

                if (objects.size() == definitions.size()) {
                    return;
                }

                if (lost.size() != 0) {
                    throw new UnmetDependencyException(
                            "unmet dependency for "
                                    + lost.get(0).getName()
                                    + " not found all dependencies"
                    );
                }
            }
        } catch (Exception e) {
            throw new DIException(e);
        }
    }
}
