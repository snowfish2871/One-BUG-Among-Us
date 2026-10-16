package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.Codec.ResultFunction;
import com.mojang.serialization.DataResult.PartialResult;
import com.mojang.serialization.MapCodec.MapCodecCodec;
import com.mojang.serialization.codecs.BaseMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.HolderSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ExtraCodecs {
   public static final Codec<JsonElement> JSON = Codec.PASSTHROUGH
      .xmap(p_253507_ -> p_253507_.convert(JsonOps.INSTANCE).getValue(), p_253513_ -> new Dynamic<>(JsonOps.INSTANCE, p_253513_));
   public static final Codec<Component> COMPONENT = adaptJsonSerializer(Component.Serializer::fromJson, Component.Serializer::toJsonTree);
   public static final Codec<Component> FLAT_COMPONENT = Codec.STRING.flatXmap(p_277276_ -> {
      try {
         return DataResult.success(Component.Serializer.fromJson(p_277276_));
      } catch (JsonParseException jsonparseexception) {
         return DataResult.error(jsonparseexception::getMessage);
      }
   }, p_277277_ -> {
      try {
         return DataResult.success(Component.Serializer.toJson(p_277277_));
      } catch (IllegalArgumentException illegalargumentexception) {
         return DataResult.error(illegalargumentexception::getMessage);
      }
   });
   public static final Codec<Vector3f> VECTOR3F = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         p_253502_ -> Util.fixedSize(p_253502_, 3).map(p_253489_ -> new Vector3f(p_253489_.get(0), p_253489_.get(1), p_253489_.get(2))),
         p_269787_ -> List.of(p_269787_.x(), p_269787_.y(), p_269787_.z())
      );
   public static final Codec<Quaternionf> QUATERNIONF_COMPONENTS = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         p_269773_ -> Util.fixedSize(p_269773_, 4).map(p_269785_ -> new Quaternionf(p_269785_.get(0), p_269785_.get(1), p_269785_.get(2), p_269785_.get(3))),
         p_269780_ -> List.of(p_269780_.x, p_269780_.y, p_269780_.z, p_269780_.w)
      );
   public static final Codec<AxisAngle4f> AXISANGLE4F = RecordCodecBuilder.create(
      p_269774_ -> p_269774_.group(
               Codec.FLOAT.fieldOf("angle").forGetter(p_269776_ -> p_269776_.angle),
               VECTOR3F.fieldOf("axis").forGetter(p_269778_ -> new Vector3f(p_269778_.x, p_269778_.y, p_269778_.z))
            )
            .apply(p_269774_, AxisAngle4f::new)
   );
   public static final Codec<Quaternionf> QUATERNIONF = withAlternative(QUATERNIONF_COMPONENTS, AXISANGLE4F.xmap(Quaternionf::new, AxisAngle4f::new));
   public static Codec<Matrix4f> MATRIX4F = Codec.FLOAT.listOf().comapFlatMap(p_269788_ -> Util.fixedSize(p_269788_, 16).map(p_269777_ -> {
         Matrix4f matrix4f = new Matrix4f();

         for(int i = 0; i < p_269777_.size(); ++i) {
            matrix4f.setRowColumn(i >> 2, i & 3, p_269777_.get(i));
         }

         return matrix4f.determineProperties();
      }), p_269775_ -> {
      FloatList floatlist = new FloatArrayList(16);

      for(int i = 0; i < 16; ++i) {
         floatlist.add(p_269775_.getRowColumn(i >> 2, i & 3));
      }

      return floatlist;
   });
   public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, p_275703_ -> "Value must be non-negative: " + p_275703_);
   public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, p_274847_ -> "Value must be positive: " + p_274847_);
   public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(
      0.0F, Float.MAX_VALUE, p_274876_ -> "Value must be positive: " + p_274876_
   );
   public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap(p_274857_ -> {
      try {
         return DataResult.success(Pattern.compile(p_274857_));
      } catch (PatternSyntaxException patternsyntaxexception) {
         return DataResult.error(() -> "Invalid regex pattern '" + p_274857_ + "': " + patternsyntaxexception.getMessage());
      }
   }, Pattern::pattern);
   public static final Codec<Instant> INSTANT_ISO8601 = temporalCodec(DateTimeFormatter.ISO_INSTANT).xmap(Instant::from, Function.identity());
   public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap(p_274852_ -> {
      try {
         return DataResult.success(Base64.getDecoder().decode(p_274852_));
      } catch (IllegalArgumentException illegalargumentexception) {
         return DataResult.error(() -> "Malformed base64 string");
      }
   }, p_216180_ -> Base64.getEncoder().encodeToString(p_216180_));
   public static final Codec<String> ESCAPED_STRING = Codec.STRING
      .comapFlatMap(p_301741_ -> DataResult.success(StringEscapeUtils.unescapeJava(p_301741_)), StringEscapeUtils::escapeJava);
   public static final Codec<ExtraCodecs.TagOrElementLocation> TAG_OR_ELEMENT_ID = Codec.STRING
      .comapFlatMap(
         p_216169_ -> p_216169_.startsWith("#")
               ? ResourceLocation.read(p_216169_.substring(1)).map(p_216182_ -> new ExtraCodecs.TagOrElementLocation(p_216182_, true))
               : ResourceLocation.read(p_216169_).map(p_216165_ -> new ExtraCodecs.TagOrElementLocation(p_216165_, false)),
         ExtraCodecs.TagOrElementLocation::decoratedId
      );
   public static final Function<Optional<Long>, OptionalLong> toOptionalLong = p_216176_ -> p_216176_.map(OptionalLong::of).orElseGet(OptionalLong::empty);
   public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = p_216178_ -> p_216178_.isPresent()
         ? Optional.of(p_216178_.getAsLong())
         : Optional.empty();
   public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM
      .xmap(p_253514_ -> BitSet.valueOf(p_253514_.toArray()), p_253493_ -> Arrays.stream(p_253493_.toLongArray()));
   private static final Codec<Property> PROPERTY = RecordCodecBuilder.create(
      p_253491_ -> p_253491_.group(
               Codec.STRING.fieldOf("name").forGetter(Property::name),
               Codec.STRING.fieldOf("value").forGetter(Property::value),
               Codec.STRING.optionalFieldOf("signature").forGetter(p_293823_ -> Optional.ofNullable(p_293823_.signature()))
            )
            .apply(p_253491_, (p_253494_, p_253495_, p_253496_) -> new Property(p_253494_, p_253495_, p_253496_.orElse(null)))
   );
   @VisibleForTesting
   public static final Codec<PropertyMap> PROPERTY_MAP = Codec.either(Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()), PROPERTY.listOf())
      .xmap(p_253515_ -> {
         PropertyMap propertymap = new PropertyMap();
         p_253515_.ifLeft(p_253506_ -> p_253506_.forEach((p_253500_, p_253501_) -> {
               for(String s : p_253501_) {
                  propertymap.put(p_253500_, new Property(p_253500_, s));
               }
            })).ifRight(p_293821_ -> {
            for(Property property : p_293821_) {
               propertymap.put(property.name(), property);
            }
         });
         return propertymap;
      }, p_253504_ -> Either.right(p_253504_.values().stream().toList()));
   private static final MapCodec<GameProfile> GAME_PROFILE_WITHOUT_PROPERTIES = RecordCodecBuilder.mapCodec(
      p_293824_ -> p_293824_.group(
               UUIDUtil.AUTHLIB_CODEC.fieldOf("id").forGetter(GameProfile::getId), Codec.STRING.fieldOf("name").forGetter(GameProfile::getName)
            )
            .apply(p_293824_, GameProfile::new)
   );
   public static final Codec<GameProfile> GAME_PROFILE = RecordCodecBuilder.create(
      p_293825_ -> p_293825_.group(
               GAME_PROFILE_WITHOUT_PROPERTIES.forGetter(Function.identity()),
               PROPERTY_MAP.optionalFieldOf("properties", new PropertyMap()).forGetter(GameProfile::getProperties)
            )
            .apply(p_293825_, (p_253518_, p_253519_) -> {
               p_253519_.forEach((p_253511_, p_253512_) -> p_253518_.getProperties().put(p_253511_, p_253512_));
               return p_253518_;
            })
   );
   public static final Codec<String> NON_EMPTY_STRING = validate(
      Codec.STRING, p_274858_ -> p_274858_.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success(p_274858_)
   );
   public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap(p_284688_ -> {
      int[] aint = p_284688_.codePoints().toArray();
      return aint.length != 1 ? DataResult.error(() -> "Expected one codepoint, got: " + p_284688_) : DataResult.success(aint[0]);
   }, Character::toString);
   public static Codec<String> RESOURCE_PATH_CODEC = validate(
      Codec.STRING,
      p_293822_ -> !ResourceLocation.isValidPath(p_293822_)
            ? DataResult.error(() -> "Invalid string to use as a resource path element: " + p_293822_)
            : DataResult.success(p_293822_)
   );

   @Deprecated
   public static <T> Codec<T> adaptJsonSerializer(Function<JsonElement, T> p_298199_, Function<T, JsonElement> p_298234_) {
      return JSON.flatXmap(p_297955_ -> {
         try {
            return DataResult.success(p_298199_.apply(p_297955_));
         } catch (JsonParseException jsonparseexception) {
            return DataResult.error(jsonparseexception::getMessage);
         }
      }, p_297961_ -> {
         try {
            return DataResult.success(p_298234_.apply(p_297961_));
         } catch (IllegalArgumentException illegalargumentexception) {
            return DataResult.error(illegalargumentexception::getMessage);
         }
      });
   }

   public static <F, S> Codec<Either<F, S>> xor(Codec<F> p_144640_, Codec<S> p_144641_) {
      return new ExtraCodecs.XorCodec<>(p_144640_, p_144641_);
   }

   public static <P, I> Codec<I> intervalCodec(
      Codec<P> p_184362_, String p_184363_, String p_184364_, BiFunction<P, P, DataResult<I>> p_184365_, Function<I, P> p_184366_, Function<I, P> p_184367_
   ) {
      Codec<I> codec = Codec.list(p_184362_).comapFlatMap(p_184398_ -> Util.fixedSize(p_184398_, 2).flatMap(p_184445_ -> {
            P p = p_184445_.get(0);
            P p1 = p_184445_.get(1);
            return p_184365_.apply(p, p1);
         }), p_184459_ -> ImmutableList.of(p_184366_.apply(p_184459_), p_184367_.apply(p_184459_)));
      Codec<I> codec1 = RecordCodecBuilder.<Pair<P, P>>create(
            p_184360_ -> p_184360_.group(p_184362_.fieldOf(p_184363_).forGetter(Pair::getFirst), p_184362_.fieldOf(p_184364_).forGetter(Pair::getSecond))
                  .apply(p_184360_, Pair::of)
         )
         .comapFlatMap(
            p_184392_ -> p_184365_.apply((P)p_184392_.getFirst(), (P)p_184392_.getSecond()),
            p_184449_ -> Pair.of(p_184366_.apply(p_184449_), p_184367_.apply(p_184449_))
         );
      Codec<I> codec2 = withAlternative(codec, codec1);
      return Codec.either(p_184362_, codec2)
         .comapFlatMap(p_184389_ -> p_184389_.map(p_184395_ -> p_184365_.apply(p_184395_, p_184395_), DataResult::success), p_184411_ -> {
            P p = p_184366_.apply(p_184411_);
            P p1 = p_184367_.apply(p_184411_);
            return Objects.equals(p, p1) ? Either.left(p) : Either.right(p_184411_);
         });
   }

   public static <A> ResultFunction<A> orElsePartial(final A p_184382_) {
      return new ResultFunction<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> p_184466_, T p_184467_, DataResult<Pair<A, T>> p_184468_) {
            MutableObject<String> mutableobject = new MutableObject<>();
            Optional<Pair<A, T>> optional = p_184468_.resultOrPartial(mutableobject::setValue);
            return optional.isPresent()
               ? p_184468_
               : DataResult.error(() -> "(" + (String)mutableobject.getValue() + " -> using default)", Pair.of(p_184382_, p_184467_));
         }

         @Override
         public <T> DataResult<T> coApply(DynamicOps<T> p_184470_, A p_184471_, DataResult<T> p_184472_) {
            return p_184472_;
         }

         @Override
         public String toString() {
            return "OrElsePartial[" + p_184382_ + "]";
         }
      };
   }

   public static <E> Codec<E> idResolverCodec(ToIntFunction<E> p_184422_, IntFunction<E> p_184423_, int p_184424_) {
      return Codec.INT
         .flatXmap(
            p_184414_ -> Optional.ofNullable(p_184423_.apply(p_184414_))
                  .map(DataResult::success)
                  .orElseGet(() -> DataResult.error(() -> "Unknown element id: " + p_184414_)),
            p_274850_ -> {
               int i = p_184422_.applyAsInt(p_274850_);
               return i == p_184424_ ? DataResult.error(() -> "Element with unknown id: " + p_274850_) : DataResult.success(i);
            }
         );
   }

   public static <E> Codec<E> stringResolverCodec(Function<E, String> p_184406_, Function<String, E> p_184407_) {
      return Codec.STRING
         .flatXmap(
            p_184404_ -> Optional.ofNullable(p_184407_.apply(p_184404_))
                  .map(DataResult::success)
                  .orElseGet(() -> DataResult.error(() -> "Unknown element name:" + p_184404_)),
            p_184401_ -> Optional.ofNullable(p_184406_.apply(p_184401_))
                  .map(DataResult::success)
                  .orElseGet(() -> DataResult.error(() -> "Element with unknown name: " + p_184401_))
         );
   }

   public static <E> Codec<E> orCompressed(final Codec<E> p_184426_, final Codec<E> p_184427_) {
      return new Codec<E>() {
         @Override
         public <T> DataResult<T> encode(E p_184483_, DynamicOps<T> p_184484_, T p_184485_) {
            return p_184484_.compressMaps() ? p_184427_.encode(p_184483_, p_184484_, p_184485_) : p_184426_.encode(p_184483_, p_184484_, p_184485_);
         }

         @Override
         public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> p_184480_, T p_184481_) {
            return p_184480_.compressMaps() ? p_184427_.decode(p_184480_, p_184481_) : p_184426_.decode(p_184480_, p_184481_);
         }

         @Override
         public String toString() {
            return p_184426_ + " orCompressed " + p_184427_;
         }
      };
   }

   public static <E> Codec<E> overrideLifecycle(Codec<E> p_184369_, final Function<E, Lifecycle> p_184370_, final Function<E, Lifecycle> p_184371_) {
      return p_184369_.mapResult(new ResultFunction<E>() {
         @Override
         public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> p_184497_, T p_184498_, DataResult<Pair<E, T>> p_184499_) {
            return p_184499_.result().map(p_184495_ -> p_184499_.setLifecycle(p_184370_.apply(p_184495_.getFirst()))).orElse(p_184499_);
         }

         @Override
         public <T> DataResult<T> coApply(DynamicOps<T> p_184501_, E p_184502_, DataResult<T> p_184503_) {
            return p_184503_.setLifecycle(p_184371_.apply(p_184502_));
         }

         @Override
         public String toString() {
            return "WithLifecycle[" + p_184370_ + " " + p_184371_ + "]";
         }
      });
   }

   public static <F, S> ExtraCodecs.EitherCodec<F, S> either(Codec<F> p_301063_, Codec<S> p_301034_) {
      return new ExtraCodecs.EitherCodec<>(p_301063_, p_301034_);
   }

   public static <K, V> ExtraCodecs.StrictUnboundedMapCodec<K, V> strictUnboundedMap(Codec<K> p_301201_, Codec<V> p_300984_) {
      return new ExtraCodecs.StrictUnboundedMapCodec<>(p_301201_, p_300984_);
   }

   public static <T> Codec<T> validate(Codec<T> p_265690_, Function<T, DataResult<T>> p_265223_) {
      return p_265690_ instanceof MapCodecCodec mapcodeccodec ? validate(mapcodeccodec.codec(), p_265223_).codec() : p_265690_.flatXmap(p_265223_, p_265223_);
   }

   public static <T> MapCodec<T> validate(MapCodec<T> p_286613_, Function<T, DataResult<T>> p_286875_) {
      return p_286613_.flatXmap(p_286875_, p_286875_);
   }

   private static Codec<Integer> intRangeWithMessage(int p_144634_, int p_144635_, Function<Integer, String> p_144636_) {
      return validate(
         Codec.INT,
         p_274889_ -> p_274889_.compareTo(p_144634_) >= 0 && p_274889_.compareTo(p_144635_) <= 0
               ? DataResult.success(p_274889_)
               : DataResult.error(() -> p_144636_.apply(p_274889_))
      );
   }

   public static Codec<Integer> intRange(int p_270883_, int p_270323_) {
      return intRangeWithMessage(p_270883_, p_270323_, p_269784_ -> "Value must be within range [" + p_270883_ + ";" + p_270323_ + "]: " + p_269784_);
   }

   private static Codec<Float> floatRangeMinExclusiveWithMessage(float p_184351_, float p_184352_, Function<Float, String> p_184353_) {
      return validate(
         Codec.FLOAT,
         p_274865_ -> p_274865_.compareTo(p_184351_) > 0 && p_274865_.compareTo(p_184352_) <= 0
               ? DataResult.success(p_274865_)
               : DataResult.error(() -> p_184353_.apply(p_274865_))
      );
   }

   public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> p_144638_) {
      return validate(p_144638_, p_274853_ -> p_274853_.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success(p_274853_));
   }

   public static <T> Codec<HolderSet<T>> nonEmptyHolderSet(Codec<HolderSet<T>> p_203983_) {
      return validate(
         p_203983_,
         p_274860_ -> p_274860_.unwrap().right().filter(List::isEmpty).isPresent()
               ? DataResult.error(() -> "List must have contents")
               : DataResult.success(p_274860_)
      );
   }

   public static <T> Codec<T> recursive(Function<Codec<T>, Codec<T>> p_298854_) {
      return new ExtraCodecs.RecursiveCodec<>(p_298854_);
   }

   public static <A> Codec<A> lazyInitializedCodec(Supplier<Codec<A>> p_184416_) {
      return new ExtraCodecs.RecursiveCodec<>(p_297963_ -> p_184416_.get());
   }

   public static <A> MapCodec<Optional<A>> strictOptionalField(Codec<A> p_299084_, String p_298710_) {
      return new ExtraCodecs.StrictOptionalFieldCodec<>(p_298710_, p_299084_);
   }

   public static <A> MapCodec<A> strictOptionalField(Codec<A> p_298828_, String p_298751_, A p_298218_) {
      return strictOptionalField(p_298828_, p_298751_)
         .xmap(p_297957_ -> p_297957_.orElse(p_298218_), p_297959_ -> Objects.equals(p_297959_, p_298218_) ? Optional.empty() : Optional.of(p_297959_));
   }

   public static <E> MapCodec<E> retrieveContext(final Function<DynamicOps<?>, DataResult<E>> p_203977_) {
      class ContextRetrievalCodec extends MapCodec<E> {
         @Override
         public <T> RecordBuilder<T> encode(E p_203993_, DynamicOps<T> p_203994_, RecordBuilder<T> p_203995_) {
            return p_203995_;
         }

         @Override
         public <T> DataResult<E> decode(DynamicOps<T> p_203990_, MapLike<T> p_203991_) {
            return p_203977_.apply(p_203990_);
         }

         @Override
         public String toString() {
            return "ContextRetrievalCodec[" + p_203977_ + "]";
         }

         @Override
         public <T> Stream<T> keys(DynamicOps<T> p_203997_) {
            return Stream.empty();
         }
      }

      return new ContextRetrievalCodec();
   }

   public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(Function<E, T> p_203985_) {
      return p_203980_ -> {
         Iterator<E> iterator = p_203980_.iterator();
         if (iterator.hasNext()) {
            T t = p_203985_.apply(iterator.next());

            while(iterator.hasNext()) {
               E e = iterator.next();
               T t1 = p_203985_.apply(e);
               if (t1 != t) {
                  return DataResult.error(() -> "Mixed type list: element " + e + " had type " + t1 + ", but list is of type " + t);
               }
            }
         }

         return DataResult.success(p_203980_, Lifecycle.stable());
      };
   }

   public static <A> Codec<A> catchDecoderException(final Codec<A> p_216186_) {
      return Codec.of(p_216186_, new Decoder<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> p_216193_, T p_216194_) {
            try {
               return p_216186_.decode(p_216193_, p_216194_);
            } catch (Exception exception) {
               return DataResult.error(() -> "Caught exception decoding " + p_216194_ + ": " + exception.getMessage());
            }
         }
      });
   }

   public static Codec<TemporalAccessor> temporalCodec(DateTimeFormatter p_216171_) {
      return Codec.STRING.comapFlatMap(p_300789_ -> {
         try {
            return DataResult.success(p_216171_.parse(p_300789_));
         } catch (Exception exception) {
            return DataResult.error(exception::getMessage);
         }
      }, p_216171_::format);
   }

   public static MapCodec<OptionalLong> asOptionalLong(MapCodec<Optional<Long>> p_216167_) {
      return p_216167_.xmap(toOptionalLong, fromOptionalLong);
   }

   public static Codec<String> sizeLimitedString(int p_265773_, int p_265217_) {
      return validate(
         Codec.STRING,
         p_274879_ -> {
            int i = p_274879_.length();
            if (i < p_265773_) {
               return DataResult.error(() -> "String \"" + p_274879_ + "\" is too short: " + i + ", expected range [" + p_265773_ + "-" + p_265217_ + "]");
            } else {
               return i > p_265217_
                  ? DataResult.error(() -> "String \"" + p_274879_ + "\" is too long: " + i + ", expected range [" + p_265773_ + "-" + p_265217_ + "]")
                  : DataResult.success(p_274879_);
            }
         }
      );
   }

   public static <T> Codec<T> withAlternative(Codec<T> p_294946_, Codec<? extends T> p_294470_) {
      return Codec.either(p_294946_, p_294470_).xmap(p_184355_ -> p_184355_.map(p_184461_ -> p_184461_, p_184455_ -> p_184455_), Either::left);
   }

   public static <T, U> Codec<T> withAlternative(Codec<T> p_295250_, Codec<U> p_295200_, Function<U, T> p_296283_) {
      return Codec.either(p_295250_, p_295200_).xmap(p_293819_ -> p_293819_.map(p_296016_ -> p_296016_, p_296283_), Either::left);
   }

   public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(Codec<T> p_298255_) {
      return Codec.unboundedMap(p_298255_, Codec.BOOL).xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
   }

   public static final class EitherCodec<F, S> implements Codec<Either<F, S>> {
      private final Codec<F> first;
      private final Codec<S> second;

      public EitherCodec(Codec<F> p_184508_, Codec<S> p_184509_) {
         this.first = p_184508_;
         this.second = p_184509_;
      }

      @Override
      public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> p_184530_, T p_184531_) {
         DataResult<Pair<Either<F, S>, T>> dataresult = this.first.decode(p_184530_, p_184531_).map(p_184524_ -> p_184524_.mapFirst(Either::left));
         if (dataresult.error().isEmpty()) {
            return dataresult;
         } else {
            DataResult<Pair<Either<F, S>, T>> dataresult1 = this.second.decode(p_184530_, p_184531_).map(p_184515_ -> p_184515_.mapFirst(Either::right));
            return dataresult1.error().isEmpty() ? dataresult1 : dataresult.apply2((p_184517_, p_184518_) -> p_184518_, dataresult1);
         }
      }

      public <T> DataResult<T> encode(Either<F, S> p_184511_, DynamicOps<T> p_184512_, T p_184513_) {
         return p_184511_.map(p_184528_ -> this.first.encode(p_184528_, p_184512_, p_184513_), p_184522_ -> this.second.encode(p_184522_, p_184512_, p_184513_));
      }

      @Override
      public boolean equals(Object p_184537_) {
         if (this == p_184537_) {
            return true;
         } else if (p_184537_ != null && this.getClass() == p_184537_.getClass()) {
            ExtraCodecs.EitherCodec<?, ?> eithercodec = (ExtraCodecs.EitherCodec)p_184537_;
            return Objects.equals(this.first, eithercodec.first) && Objects.equals(this.second, eithercodec.second);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.first, this.second);
      }

      @Override
      public String toString() {
         return "EitherCodec[" + this.first + ", " + this.second + "]";
      }
   }

   static class RecursiveCodec<T> implements Codec<T> {
      private final Supplier<Codec<T>> wrapped;
      // Neo: cache name in the constructor to make sure that we call wrapped.toString() before the memoized supplier is called.
      // Neo: as of MC 1.20.2, this is necessary for StructureTemplatePool.DIRECT_CODEC.toString() to not cause a stack overflow after loading a world once.
      private final String name;

      RecursiveCodec(Function<Codec<T>, Codec<T>> p_298813_) {
         this.wrapped = Suppliers.memoize(() -> p_298813_.apply(this));
         this.name = "RecursiveCodec[" + this.wrapped + "]";
      }

      @Override
      public <S> DataResult<Pair<T, S>> decode(DynamicOps<S> p_298957_, S p_298353_) {
         return this.wrapped.get().decode(p_298957_, p_298353_);
      }

      @Override
      public <S> DataResult<S> encode(T p_299151_, DynamicOps<S> p_299238_, S p_298526_) {
         return this.wrapped.get().encode(p_299151_, p_299238_, p_298526_);
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   static final class StrictOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
      private final String name;
      private final Codec<A> elementCodec;

      public StrictOptionalFieldCodec(String p_298270_, Codec<A> p_299308_) {
         this.name = p_298270_;
         this.elementCodec = p_299308_;
      }

      @Override
      public <T> DataResult<Optional<A>> decode(DynamicOps<T> p_298794_, MapLike<T> p_298815_) {
         T t = p_298815_.get(this.name);
         return t == null ? DataResult.success(Optional.empty()) : this.elementCodec.parse(p_298794_, t).map(Optional::of);
      }

      public <T> RecordBuilder<T> encode(Optional<A> p_298834_, DynamicOps<T> p_298455_, RecordBuilder<T> p_298535_) {
         return p_298834_.isPresent() ? p_298535_.add(this.name, this.elementCodec.encodeStart(p_298455_, p_298834_.get())) : p_298535_;
      }

      @Override
      public <T> Stream<T> keys(DynamicOps<T> p_298727_) {
         return Stream.of(p_298727_.createString(this.name));
      }

      @Override
      public boolean equals(Object p_298669_) {
         if (this == p_298669_) {
            return true;
         } else if (!(p_298669_ instanceof ExtraCodecs.StrictOptionalFieldCodec)) {
            return false;
         } else {
            ExtraCodecs.StrictOptionalFieldCodec<?> strictoptionalfieldcodec = (ExtraCodecs.StrictOptionalFieldCodec)p_298669_;
            return Objects.equals(this.name, strictoptionalfieldcodec.name) && Objects.equals(this.elementCodec, strictoptionalfieldcodec.elementCodec);
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.name, this.elementCodec);
      }

      @Override
      public String toString() {
         return "StrictOptionalFieldCodec[" + this.name + ": " + this.elementCodec + "]";
      }
   }

   public static record StrictUnboundedMapCodec<K, V>(Codec<K> keyCodec, Codec<V> elementCodec) implements Codec<Map<K, V>>, BaseMapCodec<K, V> {
      @Override
      public <T> DataResult<Map<K, V>> decode(DynamicOps<T> p_301018_, MapLike<T> p_301263_) {
         Builder<K, V> builder = ImmutableMap.builder();

         for(Pair<T, T> pair : p_301263_.entries().toList()) {
            DataResult<K> dataresult = this.keyCodec().parse(p_301018_, pair.getFirst());
            DataResult<V> dataresult1 = this.elementCodec().parse(p_301018_, pair.getSecond());
            DataResult<Pair<K, V>> dataresult2 = dataresult.apply2stable(Pair::of, dataresult1);
            if (dataresult2.error().isPresent()) {
               return DataResult.error(() -> {
                  PartialResult<Pair<K, V>> partialresult = dataresult2.error().get();
                  String s;
                  if (dataresult.result().isPresent()) {
                     s = "Map entry '" + dataresult.result().get() + "' : " + partialresult.message();
                  } else {
                     s = partialresult.message();
                  }

                  return s;
               });
            }

            if (!dataresult2.result().isPresent()) {
               return DataResult.error(() -> "Empty or invalid map contents are not allowed");
            }

            Pair<K, V> pair1 = dataresult2.result().get();
            builder.put(pair1.getFirst(), pair1.getSecond());
         }

         Map<K, V> map = builder.build();
         return DataResult.success(map);
      }

      @Override
      public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> p_301258_, T p_301052_) {
         return p_301258_.getMap(p_301052_)
            .setLifecycle(Lifecycle.stable())
            .flatMap(p_301208_ -> this.decode(p_301258_, p_301208_))
            .map(p_300941_ -> Pair.of(p_300941_, p_301052_));
      }

      public <T> DataResult<T> encode(Map<K, V> p_301101_, DynamicOps<T> p_301252_, T p_301326_) {
         return this.encode(p_301101_, p_301252_, p_301252_.mapBuilder()).build(p_301326_);
      }

      @Override
      public String toString() {
         return "StrictUnboundedMapCodec[" + this.keyCodec + " -> " + this.elementCodec + "]";
      }
   }

   public static record TagOrElementLocation(ResourceLocation id, boolean tag) {
      @Override
      public String toString() {
         return this.decoratedId();
      }

      private String decoratedId() {
         return this.tag ? "#" + this.id : this.id.toString();
      }
   }

   static record XorCodec<F, S>(Codec<F> first, Codec<S> second) implements Codec<Either<F, S>> {
      @Override
      public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> p_144679_, T p_144680_) {
         DataResult<Pair<Either<F, S>, T>> dataresult = this.first.decode(p_144679_, p_144680_).map(p_144673_ -> p_144673_.mapFirst(Either::left));
         DataResult<Pair<Either<F, S>, T>> dataresult1 = this.second.decode(p_144679_, p_144680_).map(p_144667_ -> p_144667_.mapFirst(Either::right));
         Optional<Pair<Either<F, S>, T>> optional = dataresult.result();
         Optional<Pair<Either<F, S>, T>> optional1 = dataresult1.result();
         if (optional.isPresent() && optional1.isPresent()) {
            return DataResult.error(
               () -> "Both alternatives read successfully, can not pick the correct one; first: " + optional.get() + " second: " + optional1.get(),
               optional.get()
            );
         } else if (optional.isPresent()) {
            return dataresult;
         } else {
            return optional1.isPresent() ? dataresult1 : dataresult.apply2((p_300790_, p_300791_) -> p_300791_, dataresult1);
         }
      }

      public <T> DataResult<T> encode(Either<F, S> p_144663_, DynamicOps<T> p_144664_, T p_144665_) {
         return p_144663_.map(p_144677_ -> this.first.encode(p_144677_, p_144664_, p_144665_), p_144671_ -> this.second.encode(p_144671_, p_144664_, p_144665_));
      }

      @Override
      public String toString() {
         return "XorCodec[" + this.first + ", " + this.second + "]";
      }
   }
}
